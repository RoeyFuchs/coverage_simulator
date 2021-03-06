import agents.*;
import loggers.Logger;
import loggers.MapLogger;
import tools.Administrator;
import tools.Map;

import org.apache.commons.cli.*;


import javax.xml.transform.stream.StreamResult;
import java.io.File;

public class Main {
    //cli flags
    private static final String LOG_FILE_FLAG = "lf";
    private static final String MAP_FILE_FLAG = "m";
    private static final String STRATEGY_FLAG = "s";
    private static final String MAX_ITER_FLAG = "l";
    private static final String MAP_LOGGER_FLAG = "lm";
    private static final String HELP_FLAG = "h";


    public static void main(String[] args) {
        //cli parse
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(Main.getCLIOptions(), args);
        } catch (Exception e) {
            System.err.println("Error while parse CLI");
            System.err.println(e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(" ", Main.getCLIOptions());
            return;
        }
        if(cmd.hasOption(HELP_FLAG)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(" ", Main.getCLIOptions());
            return;
        }
        //check if map and strategy were set
        if (!cmd.hasOption(MAP_FILE_FLAG))
            System.err.println("Map file didn't set");
        if (!cmd.hasOption(STRATEGY_FLAG))
            System.err.println("Strategy didn't set");

        //create map object
        Map map = null;
        try {
            map = Map.CreateMap(cmd.getOptionValue(MAP_FILE_FLAG));
        } catch (Exception e) {
            System.err.println("Error while creating map");
            return;
        }

        //create agent
        Agent agent = createAgent(map, cmd.getOptionValue(STRATEGY_FLAG));
        if (agent == null) {
            System.err.println("Strategy didn't recognize");
            return;
        }

        //logger (of path)
        StreamResult result;
        if (cmd.hasOption(LOG_FILE_FLAG)) {
            result = new StreamResult(new File(cmd.getOptionValue(LOG_FILE_FLAG)));
        } else {
            result = new StreamResult(System.out);
        }
        Logger logger = new Logger(result);
        agent.addObserver(logger);

        //logger (of map)
        String mapLoggerFile = "";
        if (cmd.hasOption(MAP_LOGGER_FLAG))
            mapLoggerFile = cmd.getOptionValue(MAP_LOGGER_FLAG);
        MapLogger mapLogger= new MapLogger(mapLoggerFile);
        map.addObserver(mapLogger);

        Administrator admin = new Administrator(map, agent);
        Long i = 1L; //counter iterations
        Long limit = getMaxIter(cmd);
        while (map.getNotReachYet() != 0 && limit > i) { //continue until coverage or limit
            admin.doOneStep();
            i++;
            //System.out.println(i);
        }
        logger.save();
        mapLogger.save();
    }

    //CLI Options
    private static Options getCLIOptions() {
        Options options = new Options();
        options.addOption(Option.builder(LOG_FILE_FLAG).hasArg().argName("file").desc("Log file. if didn't set - will print to console.").build());
        options.addOption(Option.builder(MAP_FILE_FLAG).hasArg().argName("file").desc("Map file.").build());
        options.addOption(Option.builder(MAP_LOGGER_FLAG).hasArg().argName("file").desc("Map log file. if didn't set - will print to console.").build());
        options.addOption(Option.builder(STRATEGY_FLAG).hasArg().argName("dfs/greedy/wavefront/random").desc("Strategy.").build());
        options.addOption(Option.builder(MAX_ITER_FLAG).hasArg().argName("number").desc("Limit for iteretions. if didn't set, won't be limit.").build());
        options.addOption(Option.builder(HELP_FLAG).desc("print this message").build());
        return options;
    }

    //agent parser
    private static Agent createAgent(Map map, String agentType) {
        if (agentType.equals("dfs"))
            return new DFSAgent(map);
        if (agentType.equals("greedy"))
            return new GreedyHeuristic(map);
        if (agentType.equals("wavefront"))
            return new WavefrontAgent(map);
        if(agentType.equals("random"))
            return new RandomAgent(map);
        return null;
    }
    //max iterations for agent, set by cli
    private static Long getMaxIter(CommandLine cmd) {
        if(cmd.hasOption(MAX_ITER_FLAG)) {
            return Long.parseLong(cmd.getOptionValue(MAX_ITER_FLAG));
        }
        return Long.MAX_VALUE;
    }

}
