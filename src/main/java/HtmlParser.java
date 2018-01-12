import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * Created by HSindhav on 2017-12-17.
 */
public class HtmlParser {

    private Properties properties;
    private static final String kIndexHTML = "index.html";
    private static String kFileSeparator;

    private static final String kXmlOutputDirectory = "xmlOutputDirectory";
    private static final String kHtmlOutputDirectory = "htmlOutputDirectory";
    private static final String kApiListing = "jsonFile";
    private static final String kApiNameFileNameListingFile = "apiFileNameMapping";

    private static final String kIntroduction = "Introduction";
    private static final String kAuthentication = "Authentication";
    private static final String kCommon = "Common";

    private File indexFile;
    private File apiListingFile;
    private File apiNameFileNameListing;
    private File htmlOutputDirFile;
    private String xmlOutputDir;
    private String currentDir;
    private JSONObject authenticationDefault;

    private JSONArray apiNameFileNameMapping;

    private final List<String> skipParts = Arrays.asList(skipPartsItems);
    private Map<String, JSONObject> jsonMapping = new HashMap<String, JSONObject>();

    private static final String kCategory = "category";
    private static final String kApiList = "api_list";
    private static final String kSubCategory = "sub_categories";
    private static final String kTitle = "title";
    private static final String kApiFilePath = "apiFilePath";
    private static final String skipPartsItems[] = {"Authentication", "Introduction", "Common"};

    public HtmlParser(Properties properties) {
        this.properties = properties;

        this.kFileSeparator = System.getProperty("file.separator");
        this.currentDir = System.getProperty("user.dir");
        this.xmlOutputDir = this.properties.getProperty(kXmlOutputDirectory);
        String htmlOutputDir = this.properties.getProperty(kHtmlOutputDirectory);

        htmlOutputDirFile = new File(this.currentDir + kFileSeparator + htmlOutputDir);
        htmlOutputDirFile.mkdir();

        if(this.xmlOutputDir == null || this.xmlOutputDir.length() ==0 ) {
            throw new RuntimeException("Property htmlOutputDirectory is not defined in config.properties, Please define it.");
        }

        indexFile = new File(currentDir + kFileSeparator + xmlOutputDir + kFileSeparator + kIndexHTML);

        apiListingFile = new File(this.properties.getProperty(kApiListing));

        apiNameFileNameListing = new File(this.properties.getProperty(kApiNameFileNameListingFile));

        apiNameFileNameMapping = new JSONArray();


    }

    public void parseHTML() throws Exception {


        /**
         * 1. Read index.html
         * 2. Get List of Parts
         * 3. For each part
         * 4.   4.1 Get List of Component Parts
         *      4.2 For each component parts
         *          4.2.1   Split component part apis in individual .HTML file
         *          4.2.2   Generate api listing for this part
         *          4.2.3   Return api listing for this part.
         *      4.3 Generate category listing for this part
         *      4.4 add component parts api listing in this part
         *      4.5 Return category listing for this part.
         * 5. Return category listing.
         */

        if(indexFile == null) {
            throw new RuntimeException("Index file can not be null");
        }

        JSONObject rootElement = new JSONObject();
        rootElement.put(kCategory, "restwebservices");

        JSONArray rootApis = new JSONArray();
        rootElement.put(kApiList, rootApis);

        JSONArray rootSubCategories = new JSONArray();
        rootElement.put(kSubCategory, rootSubCategories);

        readAllParts(rootElement);
        writeAndCloseFile(apiListingFile, rootElement.toString());
        //InputStream is = new FileInputStream(indexFile);

        writeAndCloseFile(apiNameFileNameListing, apiNameFileNameMapping.toString());

    }

    private JSONObject readAllParts(JSONObject rootJsonObject) throws Exception {

        Document document = Jsoup.parse(indexFile, "UTF-8");
        if(document == null) {
            throw new RuntimeException("Unable to parse index.html for a given document");
        }

        JSONArray rootSubCategories = rootJsonObject.getJSONArray(kSubCategory);

        Elements parts = document.getElementsByClass("part");
        if(parts != null && !parts.isEmpty()) {
            Element rwsPartCollection = parts.get(0);
            Element rwsPartUL = rwsPartCollection.children().get(1);
            if(rwsPartUL != null) {
                Elements rwsPartLIs = rwsPartUL.children();
                if(rwsPartLIs != null && !rwsPartLIs.isEmpty()) {
                    for(Element partChildLI : rwsPartLIs) {
                        Elements partLIChilds = partChildLI.children();
                        if(partLIChilds != null && !partLIChilds.isEmpty()) {
                            Element partA = partLIChilds.get(0);
                            String partName = partA.text();
                            String partFileName = partA.attr("href");

                            File partfile = new File(htmlOutputDirFile, partName);
                            partfile.mkdir();

                            JSONObject categoryObject = new JSONObject();
                            categoryObject.put(kCategory, partName);

                            JSONArray moduleApis = new JSONArray();
                            categoryObject.put(kApiList, moduleApis);

                            JSONArray partComponents = new JSONArray();
                            categoryObject.put(kSubCategory, partComponents);
                            rootSubCategories.put(categoryObject);
                            if(skipParts.contains(partName)) {
                                initializeDefaultApis(categoryObject);
                                continue;
                            }

                            if(partLIChilds.size() == 2) {
                                Element partItemsUL = partLIChilds.get(1);
                                Elements partItemsLIs = partItemsUL.getElementsByTag("li");
                                if(partItemsLIs != null && !partItemsLIs.isEmpty()) {
                                    for(Element componentLi : partItemsLIs) {
                                        Element componentLiA = componentLi.getElementsByTag("a").get(0);
                                        String componentFilePath = componentLiA.attr("href");
                                        String componentName = componentLiA.text();
                                        PartDetail partDetail = new PartDetail(componentName, componentFilePath);
                                        readPartDetails(partDetail, partfile, categoryObject);
                                    }
                                }
                            } else {
                                String partFileReference = partA.attr("href");
                                PartDetail partDetail = new PartDetail(partName, partFileReference);
                                //readPartDetailsTest(partDetail, partfile, categoryObject);

                            }
                        }
                    }
                }

            }
        }
        return null;
    }

    private void initializeDefaultApis(JSONObject categoryObject) throws Exception {
        if(categoryObject != null) {
            String partName = categoryObject.get(kCategory).toString();
            JSONArray introductionApis = categoryObject.getJSONArray(kApiList);
            if(kIntroduction.equals(partName)) {
                JSONObject api = new JSONObject();

                api.put(kTitle, "Before you begin");
                api.put(kApiFilePath, "." + kFileSeparator + partName + kFileSeparator + "Before you begin.html");
                introductionApis.put(api);

                api = new JSONObject();
                api.put(kTitle, "Example");
                api.put(kApiFilePath, "." + kFileSeparator + partName + kFileSeparator + "Example (a typical client side program).html");
                introductionApis.put(api);

                api = new JSONObject();
                api.put(kTitle, "Some Quick Info");
                api.put(kApiFilePath, "." + kFileSeparator +  partName + kFileSeparator + "Some Quick Info.html");
                introductionApis.put(api);

                api = new JSONObject();
                api.put(kTitle, "Usage");
                api.put(kApiFilePath, "." + kFileSeparator +  partName + kFileSeparator + "Usage.html");
                introductionApis.put(api);

            } else if(kAuthentication.equals(partName)) {
                JSONObject api = new JSONObject();
                api.put(kTitle, "Using Certificate based Authentication");
                api.put(kApiFilePath, "." + kFileSeparator + partName + kFileSeparator + "Using Certificate based Authentication.html");
                introductionApis.put(api);

                api = new JSONObject();
                api.put(kTitle, "Using OAuth 2.0");
                api.put(kApiFilePath, "." + kFileSeparator + partName + kFileSeparator + "Using OAuth 2.0.html");
                introductionApis.put(api);

            } else if(kCommon.equals(partName)) {
                JSONObject api = new JSONObject();
                api.put(kTitle, "Extracting Component Privileges");
                api.put(kApiFilePath, "." + kFileSeparator + partName + kFileSeparator + "restwebservices-common-concepts.html");
                introductionApis.put(api);
            }
            categoryObject.put(kApiList, introductionApis);
            JSONArray subCategories = new JSONArray();
        }
    }

    private JSONObject readPartDetails(PartDetail partDetail, File parentFile, JSONObject modulePartJson) throws Exception {
        if(partDetail != null) {
            String partComponentName = partDetail.getPartName();
            String partFileName = partDetail.getPartFileName().trim();
            partFileName = partFileName.replace("%20", " ");
            if(partComponentName != null || !skipParts.contains(partComponentName)) {
                File partFile = new File( currentDir + kFileSeparator + xmlOutputDir + kFileSeparator + partFileName);
                Document partDocument = Jsoup.parse(partFile, "UTF-8");
                JSONObject componentObj = new JSONObject();
                componentObj.put(kCategory, partComponentName);
                JSONArray componentSubCategories = new JSONArray();

                componentObj.put(kSubCategory, componentSubCategories);

                File partComponentNameOutDirFile = new File(parentFile.getAbsolutePath() + "/" + partComponentName);

                partComponentNameOutDirFile.mkdir();
                splitFile(partComponentNameOutDirFile, partFile, componentObj);
                JSONArray moduleJsonArray = modulePartJson.getJSONArray(kSubCategory);
                moduleJsonArray.put(componentObj);
            }
        }
        return modulePartJson;
    }

    private JSONObject splitFile(File parentFolder, File htmlContentFile, JSONObject partJson) throws Exception {
        Document document = Jsoup.parse(htmlContentFile, "UTF-8");

        Element rootArticleElement = document.getElementsByTag(("article")).get(0);
        Elements articleElements = rootArticleElement.getElementsByTag("article");
        JSONArray apisArray = new JSONArray();
        boolean firstMostAPI = true;
        if(articleElements != null && !articleElements.isEmpty()) {
            Iterator<Element> itr = articleElements.iterator();
            while(itr.hasNext()) {
                Element articleElement = itr.next();
                Element childPageHtml = getChildPage(document);
                Element bodyElement = document.createElement("body");


                Element h2Elements = articleElement.getElementsByTag("h2") != null && !articleElement.getElementsByTag("h2").isEmpty()  ? articleElement.getElementsByTag("h2").get(0) : null;
                Element articleBodyElement = null;
                if(firstMostAPI) {
                    articleBodyElement = articleElement.getElementsByTag("div") != null && !articleElement.getElementsByTag("div").isEmpty() ? articleElement.getElementsByTag("div").get(2) : null;
                    firstMostAPI = false;
                } else {
                    articleBodyElement = articleElement.getElementsByTag("div") != null && !articleElement.getElementsByTag("div").isEmpty() ? articleElement.getElementsByTag("div").get(0) : null;
                }
                String content = null;
                String title = "";
                String url = "";
                String method = "";
                if(h2Elements != null) {
                    title = h2Elements.text();
                    content = convertToCamelCase(h2Elements.outerHtml()) + "\n";
                }

                //Add breadcrumb to child html page content.
                //bodyElement = addBreadCrumb(document, bodyElement, parentFolder, title);
                if(articleBodyElement != null) {
                    if(articleBodyElement != null) {
                        Elements tables = articleBodyElement.getElementsByTag("table");
                        if(tables != null && !tables.isEmpty()){
                            Iterator<Element> tableItr = tables.iterator();
                            while(tableItr.hasNext()) {
                                Element tableElement = tableItr.next();
                                tableElement.removeAttr("class");
                                tableElement.attr("border", "1px");
                                Attribute styleAttribute = new Attribute("style", "line-height:28px;border:1px solid #c1baba;");
                                Attributes tableAttributes = tableElement.attributes() != null ? tableElement.attributes() : new Attributes();
                                tableAttributes.put(styleAttribute);

                                /*Elements tableHeading = tableElement.getElementsByTag("thead");
                                if(tableHeading != null && !tableHeading.isEmpty()) {
                                    Iterator<Element> headElementItr = tableHeading.iterator();
                                    while(headElementItr.hasNext()) {
                                        Element headingEle = headElementItr.next();
                                        Attributes headingStyles = headingEle.attributes();
                                        Attribute headingStyleAttr = new Attribute("style", "border-bottom:1px dashed;");
                                        headingStyles.put(headingStyleAttr);
                                    }
                                }
                                */
                                //Remove row rowsep-1 class from Table rows
                                Elements tableRowElements = tableElement.getElementsByTag("tr");
                                if(tableRowElements != null && !tableRowElements.isEmpty()) {
                                    for(Element row : tableRowElements) {
                                        row.removeAttr("class");
                                    }
                                }
                            }
                        }
                        Elements sections = articleBodyElement.getElementsByTag("h3");
                        if(sections != null && !sections.isEmpty()) {
                            Iterator<Element> sectionItr = sections.iterator();
                            while(sectionItr.hasNext()) {
                                Element sectionEle = sectionItr.next();
                                Attribute styleAttribute = new Attribute("style", "color : darkcyan;letter-spacing: 1px;margin-bottom:5px;");
                                Attributes sectionHeadingAttributes = sectionEle.attributes() != null ? sectionEle.attributes() : new Attributes();
                                sectionHeadingAttributes.put(styleAttribute);
                            }
                        }
                    }
                    content = content + articleBodyElement.outerHtml();
                    if(h2Elements == null) {
                        continue;
                    }
                    url = getSectionValue(articleBodyElement, "URL");
                    method = getSectionValue(articleBodyElement, "Method");
                    bodyElement.appendChild(h2Elements);
                    bodyElement.appendChild(articleBodyElement);
                }

                childPageHtml.appendChild(bodyElement);
                //String path = title != null ? title.replace(" ", "-") : "";
                //path = "/"  + path.toLowerCase();
                //String methodName = getMethodName(articleBodyElement.getElementsByTag("section"));

                String subFileName = h2Elements == null ? null : h2Elements.text();
                if(subFileName != null && subFileName != "" && content != null) {
                    subFileName = convertToCamelCase(subFileName);
                    subFileName = subFileName.replace("/", " ");
                    //subFileName = subFileName.replace(" ", "-");
                    //subFileName = kJekyllFilePrefixDate + subFileName.replace("/", "-");
                    File chunkFile = new File(parentFolder, subFileName + ".html");


                    String childPageContent = childPageHtml.outerHtml();
                    childPageContent = childPageContent.replace("h3", "h5");
                    childPageContent = childPageContent.replace("h2", "h4");

                    writeAndCloseFile(chunkFile, childPageContent);

                    // API Listing Generation
                    JSONObject apiEntry = new JSONObject();
                    String camelCaseTitle = convertToCamelCase(title);
                    apiEntry.put(kTitle, camelCaseTitle);
                    apiEntry.put(kApiFilePath, ".\\" + chunkFile.getAbsolutePath().substring(chunkFile.getAbsolutePath().indexOf("out")));
                    apisArray.put(apiEntry);
                    //apiEntry.put(kPath, path);
                    //apiEntry.put(kType, methodName == null ? "" : methodName);
                    //createAPIListingEntry(parentFolder.getName(), apiEntry);

                    //Create ApiName FileName mapping list
                    JSONObject obj = new JSONObject();
                    obj.put("apiName", camelCaseTitle);
                    obj.put(kApiFilePath, ".\\" + chunkFile.getAbsolutePath().substring(chunkFile.getAbsolutePath().indexOf("out")));
                    obj.put("component", parentFolder.getName());
                    obj.put("url", url);
                    obj.put("method", method);
                    obj.put("module", parentFolder.getParentFile() != null ? parentFolder.getParentFile().getName() : parentFolder.getName());
                    apiNameFileNameMapping.put(obj);
                }
            }
        }
        partJson.put(kApiList, apisArray);
        return partJson;
    }

    private String getSectionValue(Element sectionBodyElement, String sectionName) throws Exception {
        Elements sections = sectionBodyElement.getElementsByTag("section");
        if(sections != null && !sections.isEmpty()) {
            Iterator<Element> sectionsItr = sections.iterator();
            while(sectionsItr.hasNext()) {
                Element section = sectionsItr.next();
                Elements sectionHeadings = section.getElementsByTag("h3");
                Elements sectionTexts = section.getElementsByTag("p");
                if(sectionHeadings != null && !sectionHeadings.isEmpty()) {
                    for (Element heading : sectionHeadings) {
                        String headingTitle = heading.text();
                        if(sectionName.equalsIgnoreCase(headingTitle)) {
                            if(sectionTexts != null && !sectionTexts.isEmpty()) {
                                String sectionTextValue = sectionTexts.get(0).text();
                                return sectionTextValue;
                            }
                        }
                    }
                }
            }
        }
        return "";
    }

    private void createAPIListingEntry(String category, JSONObject entryObject) throws Exception {
        if(category != null && category != "") {

            category = convertToCamelCase(category);
            JSONObject jsonObject = jsonMapping.get(category);
            JSONArray apiList = jsonObject.getJSONArray(kApiList);
            apiList.put(entryObject);
        }
    }

    private void writeAndCloseFile(File outputFile, String content) throws Exception {
        OutputStream outputStream = new FileOutputStream(outputFile);
        outputStream.write(content.getBytes());
        outputStream.close();
    }

    private String convertToCamelCase(String str) {
        if(str != null && str != "") {
            str = str.trim();
            str = str.toLowerCase();
            String firstChar = str.substring(0, 1);
            if(firstChar!= null && firstChar.equalsIgnoreCase("<")) {
                String tmp = str.substring(str.indexOf(">") + 1, str.lastIndexOf("<"));
                firstChar = tmp.substring(0, 1);
                tmp = firstChar + tmp.substring(1);
                str = str.replace(tmp.toLowerCase(), tmp);

            }
            str = firstChar.toUpperCase() + str.substring(1);
        }
        return str;
    }

    private Element getChildPage(Document document) throws Exception {
        Tag html = Tag.valueOf("html");
        Element childPageHtml = new Element(html, "harshad");
        Element head = document.getElementsByTag("head").get(0).clone();

        Element bootstraplink = document.createElement("link");
        Attributes linkAttributes = bootstraplink.attributes();
        linkAttributes.put("href", "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css");
        linkAttributes.put("rel", "stylesheet");
        head.appendChild(bootstraplink);

        Element fontAwesomelink = document.createElement("link");
        Attributes  fontAwesomelinkAttributes = fontAwesomelink.attributes();
        fontAwesomelinkAttributes.put("href", "https://fonts.googleapis.com/css?family=Roboto:300,400,500,700,400italic");
        fontAwesomelinkAttributes.put("rel", "stylesheet");
        head.appendChild(fontAwesomelink);

        /*Element customStyleLink = document.createElement("link");
        Attributes  customStyleLinkAttributes = customStyleLink.attributes();
        customStyleLinkAttributes.put("href", "./../../../styles.css");
        customStyleLinkAttributes.put("rel", "stylesheet");
        head.appendChild(customStyleLink);
        */

        Element customStyleLink = document.createElement("style");
        String styleStr = "table tbody tr td {\n" +
                "\t\tpadding-left:10px;\n" +
                "\t}";
        customStyleLink.append(styleStr);
        customStyleLink.append("table thead tr th {padding-left:10px;}");
        customStyleLink.append("table {border:1px solid #c1baba; width:100%;}");
        customStyleLink.append("section { padding-top:10px;}");
        customStyleLink.append("section p { margin:0px;}");
        customStyleLink.append("table thead {border-bottom: 2px solid #c1baba}");
        head.appendChild(customStyleLink);
        childPageHtml.appendChild(head);
        return childPageHtml;
    }
}

class PartDetail {
    private String partName;
    private String partFileName;

    PartDetail(String partName, String partFileName) {
        this.partName = partName;
        this.partFileName = partFileName;
    }

    public String getPartName() {
        return partName;
    }

    public void setPartName(String partName) {
        this.partName = partName;
    }

    public String getPartFileName() {
        return partFileName;
    }

    public void setPartFileName(String partFileName) {
        this.partFileName = partFileName;
    }
}
