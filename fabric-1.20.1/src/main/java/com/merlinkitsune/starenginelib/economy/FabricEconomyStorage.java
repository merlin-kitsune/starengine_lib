package com.merlinkitsune.starenginelib.economy;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import com.merlinkitsune.starenginelib.StarEngineLib;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;

/**
 * 钱包余额的 Fabric 1.20.1 存储实现（语义与另三线的 {@code *EconomyStorage} 等价）。
 *
 * <h2>平台差异（必须登记）</h2>
 * <ul>
 *   <li><b>没有 {@code Entity#getPersistentData()}</b> —— 那是 Forge/NeoForge 的补丁
 *       （javap 实证：1.20.1 原版 {@code Entity} / {@code BlockEntity} 均无该方法）。
 *       本线改用 <b>Cardinal Components API</b> 的实体组件承担持久化：组件随实体 NBT 自动存读。</li>
 *   <li><b>玩家 .dat 里的根段是 {@code cardinal_components}</b>（Forge 侧为 {@code ForgeData}、
 *       NeoForge 侧为 {@code NeoForgeData}）⇒ 离线读档的路径与另三线不同（见 {@link #readOfflineWallet}）。</li>
 *   <li><b>死亡重生复制</b>由 CCA 的 {@link RespawnCopyStrategy#ALWAYS_COPY} 承担
 *       （Forge/NeoForge 侧是显式监听 {@code PlayerEvent.Clone}）。</li>
 *   <li>命令注册走 FAPI {@code CommandRegistrationCallback}（Forge 侧为 {@code RegisterCommandsEvent}）。</li>
 * </ul>
 *
 * <h2>NBT 布局</h2>
 * <pre>
 *   玩家 .dat
 *     └─ cardinal_components
 *          └─ starengine_lib:star_coin_wallet      ← 本组件的 id
 *               ├─ balance : Long
 *               └─ name    : String
 * </pre>
 * <b>不注册任何注册表条目</b>：组件挂在实体上，不占用任何 {@code ResourceLocation}
 * 归属之外的注册表位置，不影响既有存档与数据包。
 */
public final class FabricEconomyStorage implements EconomyStorage, EntityComponentInitializer {

    private static final String BALANCE_KEY = "balance";
    private static final String NAME_KEY = "name";
    /** CCA 把实体全部组件写进玩家 {@code .dat} 的这个根段。 */
    private static final String CCA_ROOT = "cardinal_components";

    /** 组件 id ⇒ 玩家 .dat 里的完整路径 = {@code cardinal_components/<id>}。 */
    public static final ResourceLocation COMPONENT_ID =
            new ResourceLocation(StarEngineLib.MODID, "star_coin_wallet");

    public static final ComponentKey<WalletComponent> WALLET =
            ComponentRegistryV3.INSTANCE.getOrCreate(COMPONENT_ID, WalletComponent.class);

    /** 钱包组件契约（余额 + 玩家名；玩家名用于离线排行榜显示）。 */
    public interface WalletComponent extends ComponentV3 {
        long balance();

        void setBalance(long balance);

        String ownerName();

        void setOwnerName(String name);
    }

    /** 在库入口构造时注入本实现。 */
    public static void install() {
        StarEngineEconomy.installStorage(new FabricEconomyStorage());
    }

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        // ALWAYS_COPY ⇒ 死亡重生 / 维度切换都复制（Forge 侧 PlayerEvent.Clone 的等价语义）。
        registry.registerForPlayers(WALLET, player -> new WalletImpl(), RespawnCopyStrategy.ALWAYS_COPY);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public long getBalance(Player player) {
        if (player == null) {
            return 0L;
        }
        return WALLET.maybeGet(player).map(WalletComponent::balance).orElse(0L);
    }

    @Override
    public void writeBalance(Player player, long balance) {
        if (player == null) {
            return;
        }
        WALLET.maybeGet(player).ifPresent(component -> {
            component.setBalance(Math.max(0L, balance));
            component.setOwnerName(player.getName().getString());
        });
    }

    @Override
    public long readOfflineBalance(MinecraftServer server, UUID playerId) {
        CompoundTag wallet = readOfflineWallet(server, playerId);
        return wallet == null ? 0L : wallet.getLong(BALANCE_KEY);
    }

    @Override
    public String readOfflineName(MinecraftServer server, UUID playerId) {
        CompoundTag wallet = readOfflineWallet(server, playerId);
        if (wallet == null) return null;
        String name = wallet.getString(NAME_KEY);
        return name.isEmpty() ? null : name;
    }

    /**
     * 离线读取钱包数据：直接解析玩家 {@code .dat}，走
     * {@code cardinal_components → starengine_lib:star_coin_wallet} 路径。
     * 任一环节缺失/解析失败一律返回 {@code null}（不抛异常，供 FTB 等第三方安全联动）。
     */
    private static CompoundTag readOfflineWallet(MinecraftServer server, UUID playerId) {
        if (server == null || playerId == null) return null;
        File dataFile = server.getWorldPath(LevelResource.PLAYER_DATA_DIR)
                .resolve(playerId + ".dat").toFile();
        if (!dataFile.isFile()) return null;
        try {
            CompoundTag root = NbtIo.readCompressed(dataFile);
            if (!root.contains(CCA_ROOT, Tag.TAG_COMPOUND)) return null;
            CompoundTag components = root.getCompound(CCA_ROOT);
            String key = COMPONENT_ID.toString();
            if (!components.contains(key, Tag.TAG_COMPOUND)) return null;
            return components.getCompound(key);
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    /** 命令注册：{@code /starcoin add|set|remove|get|rank}。 */
    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                StarCoinCommand.register(dispatcher, (source, level) -> source.hasPermission(level)));
    }

    /** CCA 组件实现：内部只存 2 个键，其余语义（钳非负、记名）由本类负责。 */
    static final class WalletImpl implements WalletComponent {
        private long balance = 0L;
        private String ownerName = "";

        @Override
        public long balance() {
            return balance;
        }

        @Override
        public void setBalance(long balance) {
            this.balance = Math.max(0L, balance);
        }

        @Override
        public String ownerName() {
            return ownerName;
        }

        @Override
        public void setOwnerName(String name) {
            this.ownerName = name == null ? "" : name;
        }

        @Override
        public void readFromNbt(CompoundTag tag) {
            this.balance = Math.max(0L, tag.getLong(BALANCE_KEY));
            this.ownerName = tag.getString(NAME_KEY);
        }

        @Override
        public void writeToNbt(CompoundTag tag) {
            tag.putLong(BALANCE_KEY, balance);
            tag.putString(NAME_KEY, ownerName);
        }
    }
}
