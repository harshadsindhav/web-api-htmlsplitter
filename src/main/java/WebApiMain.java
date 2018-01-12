import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by HSindhav on 2018-01-04.
 */
public class WebApiMain {
    private static final String kPropertyFile = "./src/main/resources/config.properties";
    private static Properties properties;

    static {
        try {
            System.out.println("Loading config properties!!");
            FileReader reader = new FileReader(kPropertyFile);
            properties = new Properties();
            properties.load(reader);

            FileWriter writer = new FileWriter("config.json");
            Gson gson = new Gson();
            gson.toJson(properties, writer);
            writer.close();

        }catch(Exception e) {
            System.out.println(e);
        }
    }
    public static void main(String argsp[]) throws  Exception {

        System.out.println("Converting XMl files to Html, it may take few moments. Please wait.");

        XmlConverter xmlConverter = new XmlConverter(properties);
        xmlConverter.convertXmlToHtml();

        System.out.println("XML to Html conversion is done successfully.");
        System.out.println("Parsing Html files for Web Api.");

        HtmlParser htmlParser = new HtmlParser(properties);
        htmlParser.parseHTML();

        System.out.println("Html parsing is done.");

        System.out.println("Transfering files to Angular App, Please wait.");

        FileTransferHelper.transferFiles(properties);

        System.out.println("Processing completed successfully.");
    }
}
