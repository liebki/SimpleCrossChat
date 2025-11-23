package de.liebki.simplecrosschatplus.commands;

import de.liebki.simplecrosschatplus.SimpleCrossChat;
import de.liebki.simplecrosschatplus.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SccCommand implements CommandExecutor, TabCompleter {

    private final SimpleCrossChat plugin;
    private final EtpCommand etpCommand;
    private final ItpCommand itpCommand;
    private final GetCommand getCommand;
    private final TransferCommand transferCommand;
    private final ComCommand comCommand;
    private final InfoCommand infoCommand;
    private final DisableCommand disableCommand;
    private final ToggleCommand toggleCommand;
    private final LocateCommand locateCommand;

    public SccCommand(SimpleCrossChat plugin) {
        this.plugin = plugin;
        this.etpCommand = new EtpCommand(plugin);
        this.itpCommand = new ItpCommand(plugin);
        this.getCommand = new GetCommand(plugin);
        this.transferCommand = new TransferCommand(plugin);
        this.comCommand = new ComCommand(plugin);
        this.infoCommand = new InfoCommand(plugin);
        this.disableCommand = new DisableCommand(plugin);
        this.toggleCommand = new ToggleCommand(plugin);
        this.locateCommand = new LocateCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "etp":
                return etpCommand.execute(sender, subArgs);
            case "itp":
                return itpCommand.execute(sender, subArgs);
            case "get":
                return getCommand.execute(sender, subArgs);
            case "transfer":
                return transferCommand.execute(sender, subArgs);
            case "com":
                return comCommand.execute(sender, subArgs);
            case "info":
                return infoCommand.execute(sender, subArgs);
            case "locate":
                return locateCommand.execute(sender, subArgs);
            case "disable":
                return disableCommand.execute(sender, subArgs);
            case "disabled":
            case "notify":
                return toggleCommand.execute(sender, subCommand, subArgs);
            default:
                sender.sendMessage(MessageUtils.ColorConvert("&cUnknown subcommand. Use /scc for help."));
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessageUtils.ColorConvert("&a&l=== SimpleCrossChatPlus ==="));
        sender.sendMessage(MessageUtils.ColorConvert("&e/scc etp <server> &7- Transfer entity to another server"));
        sender.sendMessage(MessageUtils.ColorConvert("&e/scc itp <server> &7- Transfer item to another server"));
        sender.sendMessage(MessageUtils.ColorConvert("&e/scc get <UID> &7- Redeem pending transfer"));
        sender.sendMessage(MessageUtils.ColorConvert("&e/scc transfer <amount> <player> &7- Transfer money"));
        sender.sendMessage(MessageUtils.ColorConvert("&e/scc com &7- List connected servers"));
        sender.sendMessage(MessageUtils.ColorConvert("&e/scc info <server> &7- Get server info"));
        sender.sendMessage(MessageUtils.ColorConvert("&e/scc locate <player> &7- Find a player's location"));
        sender.sendMessage(MessageUtils.ColorConvert("&e/scc disabled &7- Toggle chat disabled"));
        sender.sendMessage(MessageUtils.ColorConvert("&e/scc notify <on|off> &7- Toggle notifications"));

        if (sender.hasPermission("sccplus.admin.disable")) {
            sender.sendMessage(MessageUtils.ColorConvert("&e/scc disable <player> &7- Disable player"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("etp", "itp", "get", "transfer", "com", "info", "locate", "disabled", "notify"));

            if (sender.hasPermission("sccplus.admin.disable")) {
                completions.add("disable");
            }
        }

        return completions;
    }

}

