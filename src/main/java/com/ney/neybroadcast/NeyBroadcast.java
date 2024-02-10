package com.ney.neybroadcast;

import com.ney.neybroadcast.Modules.Broadcast.Commands;
import com.ney.neybroadcast.Modules.TabComplete.TabComplete;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class NeyBroadcast extends JavaPlugin {

    private static NeyBroadcast instance;
    private Economy economy;

    @Override
    public void onEnable() {

        instance = this;
        saveDefaultConfig();

        TabComplete tabComplete = new TabComplete();
        Commands commands = new Commands();

        getCommand("bc").setTabCompleter(tabComplete);
        getCommand("bc").setExecutor(commands);
        getCommand("ad").setTabCompleter(tabComplete);
        getCommand("ad").setExecutor(commands);
        getCommand("buy").setTabCompleter(tabComplete);
        getCommand("buy").setExecutor(commands);

        setupVault();
    }

    private void setupVault() {

        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {

            getLogger().warning("Vault not found! Make sure you have Vault installed.");
            return;

        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {

            getLogger().warning("Economy provider not found! Make sure you have an economy plugin with Vault support.");
            return;

        }

        economy = rsp.getProvider();
        getLogger().info("Vault connected successfully!");

    }

    public static NeyBroadcast getInstance() { return NeyBroadcast.instance; }

    public Economy getEconomy() { return economy; }

}