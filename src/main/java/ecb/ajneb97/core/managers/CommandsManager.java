package ecb.ajneb97.core.managers;

import ecb.ajneb97.core.model.ConfigStructure;
import ecb.ajneb97.core.model.CustomCommandGroup;
import ecb.ajneb97.core.model.TabCommandList;
import ecb.ajneb97.core.model.internal.UseCommandResult;
import org.simpleyaml.configuration.file.YamlFile;

import java.util.ArrayList;
import java.util.List;

public  class CommandsManager {
    private ConfigStructure configStructure;

    public CommandsManager(YamlFile config){
        load(config);
    }

    public void load(YamlFile config){
        List<String> commands = config.getStringList("commands");
        List<String> blockedCommandDefaultActions = config.getStringList("blocked_command_default_actions");
        List<TabCommandList> tabCommands = new ArrayList<TabCommandList>();
        for(String key : config.getConfigurationSection("tab").getKeys(false)){
            List<String> tab = config.getStringList("tab."+key+".commands");
            int priority = config.getInt("tab."+key+".priority");
            String extendTabName = config.getString("tab."+key+".extends");
            TabCommandList tabCommandList = new TabCommandList(key,priority,tab,extendTabName);
            tabCommands.add(tabCommandList);
        }
        boolean useCommandsAsWhitelist = config.getBoolean("use_commands_as_whitelist");

        List<CustomCommandGroup> customCommandGroupList = new ArrayList<CustomCommandGroup>();

        if(config.contains("custom_commands_actions")){
            for(String key : config.getConfigurationSection("custom_commands_actions").getKeys(false)){
                String path = "custom_commands_actions."+key;
                List<String> commandsList = config.getStringList(path+".commands");
                List<String> actionsList = config.getStringList(path+".actions");
                CustomCommandGroup customCommandGroup = new CustomCommandGroup(commandsList,actionsList);
                customCommandGroupList.add(customCommandGroup);
            }
        }
        configStructure = new ConfigStructure(commands,blockedCommandDefaultActions,tabCommands,useCommandsAsWhitelist
                ,customCommandGroupList);
    }

    public List<String> getBlockCommandDefaultActions(){
        return configStructure.getBlockedCommandActions();
    }

    public List<String> getTabCommands(List<String> permissions){
        List<TabCommandList> tabCommandLists = configStructure.getTabCommandList();
        List<String> currentTabCommands = null;
        List<String> defaultTabCommands = null;
        int currentPriority = -1;
        for(TabCommandList t : tabCommandLists){
            if(t.getName().equals("default")){
                defaultTabCommands = t.getCommands();
                continue;
            }

            String perm = t.getPermission();
            if(permissions.contains(perm)){
                if(t.getPriority() > currentPriority){
                    currentTabCommands = t.getCommands();
                    currentPriority = t.getPriority();

                    if(t.getExtendTabName() != null){
                        currentTabCommands.addAll(getExtendsTabCommands(t.getExtendTabName(),
                                new ArrayList<String>(),0));
                    }
                }
            }
        }

        if(currentTabCommands != null){
            return currentTabCommands;
        }else{
            return defaultTabCommands;
        }
    }

    public List<String> getExtendsTabCommands(String name,List<String> extendsTabCommandList,int currentIteration){
        if(currentIteration >= 10){
            //In case of stackoverflow
            return new ArrayList<String>();
        }
        List<TabCommandList> tabCommandLists = configStructure.getTabCommandList();
        TabCommandList tabCommandList = getTabCommandListByName(name,tabCommandLists);

        extendsTabCommandList.addAll(tabCommandList.getCommands());

        if(tabCommandList.getExtendTabName() != null){
            currentIteration++;
            return getExtendsTabCommands(tabCommandList.getExtendTabName(),extendsTabCommandList,currentIteration);
        }else{
            return extendsTabCommandList;
        }
    }

    public TabCommandList getTabCommandListByName(String name,List<TabCommandList> tabCommandLists){
        for(TabCommandList t : tabCommandLists){
            if(t.getName().equals(name)){
                return t;
            }
        }
        return null;
    }

    public UseCommandResult useCommand(String command){

        String[] commandWithArgs = command.toLowerCase().split(" ");
        for(String blockedCommand : configStructure.getCommands()){
            String[] blockedCommandWithArgs = blockedCommand.toLowerCase().split(" ");
            int equalArguments = 0;
            for(int i=0;i<blockedCommandWithArgs.length;i++){
                if(i > commandWithArgs.length-1){
                    break;
                }
                String currentArg = commandWithArgs[i];
                if(currentArg.equals(blockedCommandWithArgs[i])){
                    equalArguments++;
                }
            }
            if(equalArguments < blockedCommandWithArgs.length){
                continue;
            }else{
                if(configStructure.isUseCommandsAsWhitelist()){
                    return new UseCommandResult(true,blockedCommand);
                }
                return new UseCommandResult(false,blockedCommand);
            }
        }
        if(configStructure.isUseCommandsAsWhitelist()){
            return new UseCommandResult(false,null);
        }
        return new UseCommandResult(true,null);
    }

    public List<String> getActionsForCustomCommand(String command){
        List<CustomCommandGroup> customCommandGroupList = configStructure.getCustomCommands();
        for(CustomCommandGroup customCommandGroup : customCommandGroupList){
            List<String> commands = customCommandGroup.getCommands();
            if(commands.contains(command)){
                return customCommandGroup.getActions();
            }
        }
        return null;
    }

    public ConfigStructure getConfigStructure() {
        return configStructure;
    }
}
