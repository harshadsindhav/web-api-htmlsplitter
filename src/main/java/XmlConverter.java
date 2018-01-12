import java.io.File;
import java.util.Properties;

/**
 * Created by HSindhav on 2018-01-04.
 */
public class XmlConverter {
    Properties properties;

    private static String kXmlSource = "xmlSource"; //= "D:\\Saba-Source\\Saba-People-Cloud\\doc_sc_qr_int_dev2";
    private static String kXmlDestination = "xmlOutputDirectory"; //= "D:\\Harshad\\Update39\\WebApiJavaTool\\in";
    private static String kDitamapFile = "ditaMapFile"; //= "sc_restapi_reference.ditamap";
    private static String kFileSeparator;

    public XmlConverter(Properties properties) {
        this.properties = properties;
        kFileSeparator = System.getProperty("file.separator");
    }

    public void convertXmlToHtml() {
        try {

            File docRepository = new File(this.properties.getProperty(kXmlSource));
            File ditaMapFile = new File(docRepository, this.properties.getProperty(kDitamapFile));

            String currentDir = System.getProperty("user.dir");
            String xmlOutputDir = currentDir + kFileSeparator + this.properties.getProperty(kXmlDestination);

            String cmdArray[] = new String[6];

            cmdArray[0] = "cmd.exe";

            cmdArray[1] = "/c";

            cmdArray[2] = "dita";

            cmdArray[3] = "--input=" + ditaMapFile.getAbsolutePath();

            cmdArray[4] = "--output=" + xmlOutputDir;

            cmdArray[5] = "--format=html5";

            Process process = Runtime.getRuntime().exec(cmdArray);
            process.waitFor();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
