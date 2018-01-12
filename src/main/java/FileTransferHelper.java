import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Properties;

/**
 * Created by HSindhav on 2018-01-04.
 */
public class FileTransferHelper {

    private static final String kHtmlOutputDirectory = "htmlOutputDirectory";
    private static final String kJsonFile = "jsonFile";
    private static final String kApiFileNameMapping = "apiFileNameMapping";
    private static final String kTargetAppDirectory = "targetAppDirectory";
    private static final String kConfigJsonFileName = "config.json";

    private static String fileSeparator;
    private static File currentDirectory;

    public static void transferFiles(Properties properties) throws Exception {
        fileSeparator = System.getProperty("file.separator");

        currentDirectory = new File(System.getProperty("user.dir"));

        String targetAppDirectoryName = properties.getProperty(kTargetAppDirectory);
        File targetAppDirectoryFile = new File(targetAppDirectoryName);

        /**
         * Transfer Config json
         */
        File configJsonFile = new File(currentDirectory, kConfigJsonFileName);
        FileUtils.copyFileToDirectory(configJsonFile, targetAppDirectoryFile);

        /**
         * Transfer HTML asset api folder to target app folder
         */
        String htmlOutputDirName = properties.getProperty(kHtmlOutputDirectory);
        File htmlOutputDirFile = new File(currentDirectory, htmlOutputDirName);

        File outputDir = new File(targetAppDirectoryFile, htmlOutputDirName);
        outputDir.mkdir();

        FileUtils.copyDirectory(htmlOutputDirFile, outputDir);

        /**
         * Transfer Json files to target app folder
         */
        String jsonFileName = properties.getProperty(kJsonFile);
        File jsonFile = new File(currentDirectory, jsonFileName);

        FileUtils.copyFileToDirectory(jsonFile, targetAppDirectoryFile);

        String apiFileNameMappingName = properties.getProperty(kApiFileNameMapping);
        File apiFileNameMappingFile = new File(currentDirectory, apiFileNameMappingName);

        /**
         * Transfer Introduction folder to target app folder
         */
        String IntroductionFolderName = "Introduction";
        File folderFile = new File(currentDirectory, IntroductionFolderName);
        File outputIntroductionDir = new File(targetAppDirectoryFile, IntroductionFolderName);
        outputIntroductionDir.mkdir();

        FileUtils.copyDirectory(folderFile, outputIntroductionDir);

        String authenticationFolderName = "Authentication";
        File authenticationFolderFile = new File(currentDirectory, authenticationFolderName);
        File outputAuthenticationDir = new File(targetAppDirectoryFile, authenticationFolderName);
        outputAuthenticationDir.mkdir();

        FileUtils.copyDirectory(authenticationFolderFile, outputAuthenticationDir);

        String commonFolderName = "Common";
        File commonFolderFile = new File(currentDirectory, commonFolderName);
        File outputCommonDir = new File(targetAppDirectoryFile, commonFolderName);
        outputCommonDir.mkdir();

        FileUtils.copyDirectory(commonFolderFile, outputCommonDir);

    }

}
