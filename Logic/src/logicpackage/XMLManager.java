package logicpackage;

import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class XMLManager {
    private final static String s_MagitBlobs = "MagitBlob";
    private final static String s_MagitFolders = "MagitSingleFolder";
    private final static String s_MagitCommits = "MagitSingleCommit";
    private final static String s_MagitBranches = "MagitSingleBranch";


    public static List<String> GetXMLFileErrors(File i_XMLFile) {
        List<String> result = new LinkedList<>();
        try {
            if (i_XMLFile.exists()) {
                if (FilenameUtils.getExtension(i_XMLFile.toString()).equals("xml")) {
                    if (isXMLIDRepeating(i_XMLFile)) {
                        result.add("Contains elements with same id");
                    }
                    result.addAll(findErrorsInXMLFolders(i_XMLFile));
                    result.addAll(findErrorsInXMLCommit(i_XMLFile));
                    result.addAll(findErrorsInXMLBranches(i_XMLFile));
                } else {
                    result.add("file not xml");
                }
            } else {
                result.add("file doesn't exist");
            }
        } catch (Exception e) {

        }

        return result;
    }

    private static List<String> findErrorsInXMLBranches(File i_XMLFile) throws IOException, SAXException, ParserConfigurationException {
        Document xml = getXMLDocument(i_XMLFile);
        NodeList branchesList = xml.getElementsByTagName(s_MagitBranches);
        String headBranchName = xml.getElementsByTagName("head").item(0).getTextContent();
        Boolean isHeadValid = false;
        List<String> errorList = new LinkedList<>();

        for (int i = 0; i < branchesList.getLength(); i++) {
            Element currentBranch = (Element) branchesList.item(i);
            String currentBranchName = currentBranch.getElementsByTagName("name").item(0).getTextContent();
            Element pointedCommit = (Element) currentBranch.getElementsByTagName("pointed-commit").item(0);
            String pointedCommitID = pointedCommit.getAttribute("id");
            Element xmlCommit = GetXMLElementByID(xml, s_MagitCommits, pointedCommitID);

            if (xmlCommit == null) {
                errorList.add(String.format("Commit id = %s in Branch name = %s invalid", pointedCommitID, currentBranchName));
            }

            if (headBranchName.equals(currentBranchName)) {
                isHeadValid = true;
            }
        }

        if (!isHeadValid) {
            errorList.add("Head branch invalid");
        }

        return errorList;
    }

    private static List<String> findErrorsInXMLCommit(File i_XMLFile) throws IOException, SAXException, ParserConfigurationException {
        Document xml = getXMLDocument(i_XMLFile);
        NodeList commitsList = xml.getElementsByTagName(s_MagitCommits);
        List<String> errorList = new LinkedList<>();

        for (int i = 0; i < commitsList.getLength(); i++) {
            Element currentCommit = (Element) commitsList.item(i);
            String currentCommitID = currentCommit.getAttribute("id");

            Element containedRootFolder = (Element) currentCommit.getElementsByTagName("root-folder").item(0);
            String containedRootFolderID = containedRootFolder.getAttribute("id");
            Element xmlFolder = GetXMLElementByID(xml, s_MagitFolders, containedRootFolderID);

            if (xmlFolder == null) {
                errorList.add(String.format("Folder id = %s in Commit id = %s invalid", containedRootFolderID, currentCommitID));
            } else if (!xmlFolder.getAttribute("is-root").equals("true")) {
                errorList.add(String.format("Folder id = %s in Commit id = %s is not root", containedRootFolderID, currentCommitID));
            }
        }

        return errorList;
    }

    public static Element GetXMLElementByID(Document i_XMLDocument, String i_Type, String i_ElementID) {
        NodeList nodeList = i_XMLDocument.getElementsByTagName(i_Type);
        Element result = null;

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element currentElement = (Element) nodeList.item(i);
            String currentElementID = currentElement.getAttribute("id");
            if (i_ElementID.equals((currentElementID))) {
                result = currentElement;
                break;
            }
        }

        return result;
    }

    private static List<String> findErrorsInXMLFolders(File i_XMLFile) throws IOException, SAXException, ParserConfigurationException {
        Document xml = getXMLDocument(i_XMLFile);
        NodeList foldersList = xml.getElementsByTagName(s_MagitFolders);
        List<String> errorList = new LinkedList<>();

        for (int i = 0; i < foldersList.getLength(); i++) {
            Element currentFolder = (Element) foldersList.item(i);
            String currentFolderID = currentFolder.getAttribute("id");
            NodeList containedItems = currentFolder.getElementsByTagName("item");
            for (int j = 0; j < containedItems.getLength(); j++) {
                Element containedItem = (Element) containedItems.item(j);
                String itemID = containedItem.getAttribute("id");
                String itemType = containedItem.getAttribute("type");
                if (itemType.equals(("blob"))) {
                    if (!isXMLElementExist(xml, containedItem, s_MagitBlobs)) {
                        errorList.add(String.format("Blob id = %s in Folder id = %s invalid", itemID, currentFolderID));
                    }
                } else if (itemType.equals("folder")) {
                    if (itemID.equals(currentFolderID)) {
                        errorList.add(String.format("Folder id = %s points to itself", currentFolderID));
                    } else if (!isXMLElementExist(xml, containedItem, s_MagitFolders)) {
                        errorList.add(String.format("Folder id = %s in Folder id = %s invalid", itemID, currentFolderID));
                    }
                }
            }
        }

        return errorList;
    }


    private static Boolean isXMLElementExist(Document i_XMLDocument, Element i_Element, String i_Type) {
        NodeList nodeList = i_XMLDocument.getElementsByTagName(i_Type);
        String elementID = i_Element.getAttribute("id");
        Boolean isExist = false;

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element currentElement = (Element) nodeList.item(i);
            String currentElementID = currentElement.getAttribute("id");
            if (elementID.equals((currentElementID))) {
                isExist = true;
                break;
            }
        }

        return isExist;
    }

    private static Boolean isXMLIDRepeating(File i_XMLFile) throws IOException, SAXException, ParserConfigurationException {
        Document xml = getXMLDocument(i_XMLFile);
        NodeList blobsList = xml.getElementsByTagName(s_MagitBlobs);
        NodeList foldersList = xml.getElementsByTagName(s_MagitFolders);
        NodeList commitsList = xml.getElementsByTagName(s_MagitCommits);

        return isSameIDInNodeList(blobsList) || isSameIDInNodeList(foldersList) || isSameIDInNodeList(commitsList);
    }

    private static Document getXMLDocument(File i_XMLFile) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory db = DocumentBuilderFactory.newInstance();
        DocumentBuilder build = db.newDocumentBuilder();
        Document xml = build.parse(i_XMLFile);

        return xml;
    }

    private static Boolean isSameIDInNodeList(NodeList entries) {
        Boolean isSameIDInXML = false;

        for (int i = 0; i < entries.getLength() && !isSameIDInXML; i++) {
            Element element1 = (Element) entries.item(i);
            String element1ID = element1.getAttribute("id");

            for (int j = 0; j < entries.getLength(); j++) {
                Element element2 = (Element) entries.item(j);
                String element2ID = element2.getAttribute("id");

                if (i != j && element1ID.equals(element2ID)) {
                    isSameIDInXML = true;
                    break;
                }
            }
        }

        return isSameIDInXML;
    }

    public static Path GetRepositoryPathFromXML(File i_XMLFile) throws IOException, SAXException, ParserConfigurationException {
        Document xmlDocument = getXMLDocument(i_XMLFile);
        String repositoryPathString = xmlDocument.getElementsByTagName("location").item(0).getTextContent();
        String repositoryName = ((Element) xmlDocument.getElementsByTagName("MagitRepository").item(0)).getAttribute("name");
        return Paths.get(repositoryPathString + "\\" + repositoryName);
    }

    public static void BuildRepositoryObjectsFromXML(File i_XMLFile, Path i_RepositoryPath) throws IOException, SAXException, ParserConfigurationException {
        Document xmlDocument = getXMLDocument(i_XMLFile);
        buildBlobsObjects(xmlDocument, i_RepositoryPath);
    }

    private static void buildBlobsObjects(Document i_XMLDocument, Path i_RepositoryPath){
        FilesManagement.CreateFolder(Paths.get(i_RepositoryPath+"\\"+FilesManagement.s_GitDirectory), FilesManagement.s_XmlBuildFolderName);
        NodeList blobNodesList = i_XMLDocument.getElementsByTagName(s_MagitBlobs);

        for(int i=0; i<blobNodesList.getLength(); i++){
            Element blobElement = (Element)blobNodesList.item(i);
            String name = blobElement.getElementsByTagName("name").item(0).getTextContent();
            String lastUpdater = blobElement.getElementsByTagName("last-updater").item(0).getTextContent();
            String lastUpdateDate = blobElement.getElementsByTagName("last-update-date").item(0).getTextContent();
            String content = blobElement.getElementsByTagName("content").item(0).getTextContent();

            //createTemporaryTaextFile(name, lastUpdater)
        }

    }

    private static Branch buildBranchFromElement(Element i_BranchElement, Path i_Path){
        return null;
    }

    private static Commit buildCommitFromElement(Element i_CommitElement, Path i_Path){
        return null;

    }

    /*private static Folder buildFolderFromElement(Element i_FolderElement, Path i_Path, Document i_XMLDocument){
        String lastUpdater = i_FolderElement.getElementsByTagName("last-updater").item(0).getTextContent();
        String lastUpdateDate = i_FolderElement.getElementsByTagName("last-Update-date").item(0).getTextContent();
        String name = i_FolderElement.getElementsByTagName("name").item(0).getTextContent();
        List<BlobData> blobList = new LinkedList<>();

        NodeList containedItems = i_FolderElement.getElementsByTagName("item");
        for(int i=0; i<containedItems.getLength(); i++){
            Element currentItem = (Element) containedItems.item(i);
            String itemID = currentItem.getAttribute("id");
            String itemType = currentItem.getAttribute("type");

            if(itemType.equals("blob")){
                Element blobElement =   GetXMLElementByID(i_XMLDocument, s_MagitBlobs, itemID);
                blobList.add(buildBlobFromElement(blobElement, i_Path));
            }else if(itemType.equals("folder")){
                Element folderElement =   GetXMLElementByID(i_XMLDocument, s_MagitFolders, itemID);
                //blobList.add(buildFolderFromElement(folderElement, i_Path, i_XMLDocument));
            }



        }

       // return

    }*/

    /*private static BlobData buildBlobFromElement(Element i_BlobElement, Path i_RepositoryPath){
        String name = i_BlobElement.getElementsByTagName("name").item(0).getTextContent();
        String lastUpdater = i_BlobElement.getElementsByTagName("last-updater").item(0).getTextContent();
        String lastUpdateDate = i_BlobElement.getElementsByTagName("last-update-date").item(0).getTextContent();
        String content = i_BlobElement.getElementsByTagName("content").item(0).getTextContent();

        //File file = new File("i_RepositoryPa")

        FilesManagement.CreateSimpleFileDescription(i_RepositoryPath, Path filePathOrigin, String i_UserName, String i_TestFolderName)

        //return new BlobData(
    }*/
}