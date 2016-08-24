package org.wzy.sqltemplate;

import org.wzy.sqltemplate.script.SqlFragment;

/**
 * Created by Cheney on 2016/8/23.
 */

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wzy.sqltemplate.Configuration;
import org.wzy.sqltemplate.SqlTemplate;
import org.wzy.sqltemplate.script.*;
import org.xml.sax.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 负责解析 XML 文件
 */
class SqlTemplateEngin {

    private Configuration cfg;
    private SqlTemplateKit.SqlTemplatePage sqlPage;

    public SqlTemplateEngin(Configuration cfg, SqlTemplateKit.SqlTemplatePage sqlPage) {
        this.cfg = cfg;
        this.sqlPage = sqlPage;
    }

    /**
     * 解析 mapper 的第一层目录
     * @return
     */
    public HashMap<String,Node> buildPage(String templateContent) throws IOException, SAXException, ParserConfigurationException {
        Document root = buildXml(templateContent);
        NodeList list = root.getElementsByTagName("mapper");
        Node mapper = list.item(0);
        NodeList children = mapper.getChildNodes();

        HashMap<String,Node> ret = new HashMap<String,Node>();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if( Node.ELEMENT_NODE == child.getNodeType() ){
                NamedNodeMap attributes = child.getAttributes();
                Node attrId = attributes
                        .getNamedItem("id");

                String id = attrId.getTextContent();
                ret.put(id,child);
            }
        }
        return ret;
    }


    /**
     * 解析 sql 条目
     * @return
     */
    public SqlTemplate buildSQLTempl(Node node) {

        List<SqlFragment> contents = buildDynamicTag(node);

        return new SqlTemplate(cfg,new MixedSqlFragment(contents));

    }

    /**
     * 解析动态标签
     * @param node
     * @return
     */
    private List<SqlFragment> buildDynamicTag(Node node) {

        List<SqlFragment> contents = new ArrayList<SqlFragment>();

        NodeList children = node.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            String nodeName = child.getNodeName();

            if (child.getNodeType() == Node.CDATA_SECTION_NODE
                    || child.getNodeType() == Node.TEXT_NODE) {

                String sql = child.getTextContent();

                TextFragment textFragment = new TextFragment(sql);

                contents.add(textFragment);

            } else if (child.getNodeType() == Node.ELEMENT_NODE) {

                TagHandler tagHandler = this.nodeHandlers.get(nodeName.toLowerCase());

                if(tagHandler != null ){
                    tagHandler.handleNode(child, contents);
                }

            }
        }

        return contents;
    }

    private Document buildXml(String templateContent)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory
                .newInstance();
        factory.setValidating(true);

        factory.setNamespaceAware(false);
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(false);
        factory.setCoalescing(false);
        factory.setExpandEntityReferences(true);

        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver(new EntityResolver() {

            public InputSource resolveEntity(String publicId,
                                             String systemId) throws SAXException, IOException {
                System.out.println("publicId :" + publicId);
                System.out.println("systemId :" + systemId);
                return new InputSource(SqlTemplate.class.getResourceAsStream("/mybatis-3-mapper.dtd"));
            }
        });
        builder.setErrorHandler(new ErrorHandler() {
            public void error(SAXParseException exception)
                    throws SAXException {
                throw exception;
            }

            public void fatalError(SAXParseException exception)
                    throws SAXException {
                throw exception;
            }

            public void warning(SAXParseException exception)
                    throws SAXException {
            }
        });

        InputSource inputSource = new InputSource(new StringReader(templateContent));

        return builder.parse(inputSource);
    }

    private Map<String, TagHandler> nodeHandlers = new HashMap<String, TagHandler>() {
        private static final long serialVersionUID = 7123056019193266281L;

        {
            put("trim", new TrimHandler());
            put("where", new WhereHandler());
            put("set", new SetHandler());
            put("foreach", new ForEachHandler());
            put("if", new IfHandler());
            put("choose", new ChooseHandler());
            put("when", new IfHandler());
            put("include", new IncludeHandler());
            put("otherwise", new OtherwiseHandler());
        }
    };

    private interface TagHandler {
        void handleNode(Node nodeToHandle, List<SqlFragment> targetContents);
    }

    private class TrimHandler implements TagHandler {
        public void handleNode(Node nodeToHandle,
                               List<SqlFragment> targetContents) {
            List<SqlFragment> contents = buildDynamicTag(nodeToHandle);
            MixedSqlFragment mixedSqlFragment = new MixedSqlFragment(
                    contents);

            NamedNodeMap attributes = nodeToHandle.getAttributes();
            Node prefixAtt = attributes
                    .getNamedItem("prefix");

            String prefix = prefixAtt == null ? null :  prefixAtt.getTextContent();

            Node prefixOverridesAtt = attributes
                    .getNamedItem("prefixOverrides");

            String prefixOverrides = prefixOverridesAtt.getTextContent();

            Node suffixAtt = attributes
                    .getNamedItem("suffix");

            String suffix = suffixAtt == null ?  null :suffixAtt.getTextContent();

            Node suffixOverridesAtt = attributes
                    .getNamedItem("suffixOverrides");

            String suffixOverrides = suffixOverridesAtt==null ? null :suffixOverridesAtt .getTextContent();
            TrimFragment trim = new TrimFragment(mixedSqlFragment, prefix,
                    suffix, prefixOverrides, suffixOverrides);
            targetContents.add(trim);
        }
    }

    private class WhereHandler implements TagHandler {
        public void handleNode(Node nodeToHandle,
                               List<SqlFragment> targetContents) {
            List<SqlFragment> contents = buildDynamicTag(nodeToHandle);
            MixedSqlFragment mixedSqlFragment = new MixedSqlFragment(
                    contents);
            WhereFragment where = new WhereFragment(mixedSqlFragment);
            targetContents.add(where);
        }
    }

    private class SetHandler implements TagHandler {
        public void handleNode(Node nodeToHandle,
                               List<SqlFragment> targetContents) {
            List<SqlFragment> contents = buildDynamicTag(nodeToHandle);
            MixedSqlFragment mixedSqlFragment = new MixedSqlFragment(
                    contents);
            SetFragment set = new SetFragment(mixedSqlFragment);
            targetContents.add(set);
        }
    }

    private class ForEachHandler implements TagHandler {
        public void handleNode(Node nodeToHandle,
                               List<SqlFragment> targetContents) {
            List<SqlFragment> contents = buildDynamicTag(nodeToHandle);
            MixedSqlFragment mixedSqlFragment = new MixedSqlFragment(
                    contents);
            NamedNodeMap attributes = nodeToHandle.getAttributes();
            Node collectionAtt= attributes.getNamedItem("collection");

            if(collectionAtt == null ){
                throw new RuntimeException(nodeToHandle.getNodeName() +" must has a collection attribute !" ) ;
            }

            String collection = collectionAtt.getTextContent();

            Node itemAtt = attributes.getNamedItem("item") ;

            String item = itemAtt == null ? "item" :itemAtt	.getTextContent();

            Node indexAtt = attributes.getNamedItem("index");

            String index = indexAtt == null ? "index" :indexAtt.getTextContent();

            Node openAtt = attributes.getNamedItem("open");

            String open = openAtt == null ? null : openAtt.getTextContent();

            Node closeAtt = attributes.getNamedItem("close");

            String close = closeAtt == null ? null : closeAtt.getTextContent();

            Node sparatorAtt = attributes.getNamedItem("separator");

            String separator = sparatorAtt == null  ? null :sparatorAtt.getTextContent();

            ForEachFragment forEachSqlFragment = new ForEachFragment(
                    mixedSqlFragment, collection, index, item, open, close,
                    separator);
            targetContents.add(forEachSqlFragment);
        }
    }

    private class IfHandler implements TagHandler {
        public void handleNode(Node nodeToHandle,
                               List<SqlFragment> targetContents) {
            List<SqlFragment> contents = buildDynamicTag(nodeToHandle);
            MixedSqlFragment mixedSqlFragment = new MixedSqlFragment(
                    contents);

            NamedNodeMap attributes = nodeToHandle.getAttributes();

            Node testAtt = attributes.getNamedItem("test") ;

            if(testAtt == null ){
                throw new RuntimeException(nodeToHandle.getNodeName()+" must has test attribute ! ") ;
            }

            String test = testAtt.getTextContent();

            IfFragment ifSqlFragment = new IfFragment(mixedSqlFragment,
                    test);
            targetContents.add(ifSqlFragment);
        }
    }

    private class OtherwiseHandler implements TagHandler {
        public void handleNode(Node nodeToHandle,
                               List<SqlFragment> targetContents) {
            List<SqlFragment> contents = buildDynamicTag(nodeToHandle);
            MixedSqlFragment mixedSqlFragment = new MixedSqlFragment(
                    contents);
            targetContents.add(mixedSqlFragment);
        }
    }

    private class IncludeHandler implements TagHandler {
        public void handleNode(Node nodeToHandle,
                               List<SqlFragment> targetContents) {
            NamedNodeMap attributes = nodeToHandle.getAttributes();
            Node refidN = attributes.getNamedItem("refid");
            String refid = refidN.getTextContent();
            // 从本页已解析的标签中载入内容
            Node node = sqlPage.getChildNode(refid);
            String content = node.getTextContent();
            IncludeFragment fragment = new IncludeFragment(content);
            targetContents.add(fragment);
        }
    }


    private class ChooseHandler implements TagHandler {
        public void handleNode(Node nodeToHandle,
                               List<SqlFragment> targetContents) {
            List<SqlFragment> whenSqlFragments = new ArrayList<SqlFragment>();
            List<SqlFragment> otherwiseSqlFragments = new ArrayList<SqlFragment>();
            handleWhenOtherwiseNodes(nodeToHandle, whenSqlFragments,
                    otherwiseSqlFragments);
            SqlFragment defaultSqlFragment = getDefaultSqlFragment(otherwiseSqlFragments);
            ChooseFragment chooseSqlFragment = new ChooseFragment(
                    whenSqlFragments, defaultSqlFragment);
            targetContents.add(chooseSqlFragment);
        }

        private void handleWhenOtherwiseNodes(Node chooseSqlFragment,
                                              List<SqlFragment> ifSqlFragments,
                                              List<SqlFragment> defaultSqlFragments) {
            NodeList children = chooseSqlFragment.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    String nodeName = child.getNodeName();
                    TagHandler handler = nodeHandlers.get(nodeName);
                    if (handler instanceof IfHandler) {
                        handler.handleNode(child, ifSqlFragments);
                    } else if (handler instanceof OtherwiseHandler) {
                        handler.handleNode(child, defaultSqlFragments);
                    }
                }
            }

        }

        private SqlFragment getDefaultSqlFragment(
                List<SqlFragment> defaultSqlFragments) {
            SqlFragment defaultSqlFragment = null;
            if (defaultSqlFragments.size() == 1) {
                defaultSqlFragment = defaultSqlFragments.get(0);
            } else if (defaultSqlFragments.size() > 1) {
                throw new RuntimeException(
                        "Too many default (otherwise) elements in choose statement.");
            }
            return defaultSqlFragment;
        }
    }

}
