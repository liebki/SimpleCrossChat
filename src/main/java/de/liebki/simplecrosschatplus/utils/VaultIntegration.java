package de.liebki.simplecrosschatplus.utils;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultIntegration {

    private static Economy economy = null;
    private static boolean vaultAvailable = false;

    public static boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        economy = rsp.getProvider();
        vaultAvailable = economy != null;
        return vaultAvailable;
    }

    public static boolean isVaultAvailable() {
        return vaultAvailable;
    }

    public static Economy getEconomy() {
        return economy;
    }

    public static boolean hasEnough(OfflinePlayer player, double amount) {
        if (!vaultAvailable) {
            return true;
        }

        return economy.has(player, amount);
    }

    public static boolean withdraw(OfflinePlayer player, double amount) {
        if (!vaultAvailable) {
            return true;
        }

        if (!economy.has(player, amount)) {
            return false;
        }

        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public static boolean deposit(OfflinePlayer player, double amount) {
        if (!vaultAvailable) {
            return true;
        }

        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public static String format(double amount) {
        if (!vaultAvailable) {
            return String.format("%.2f", amount);
        }

        return economy.format(amount);
    }

}

