package org.wzy.sqltemplate;

import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by Cheney on 2016/8/24.
 */
public class TestSqlTempl {

    @Test
    public void testXml() throws IOException, ParserConfigurationException, SAXException {
        SqlTemplate sqlTemplate = SqlTemplateKit.getDefIns().getTemplate("Service#selectByPrimaryKey");

        HashMap<String, Object> map = new HashMap<String, Object>();

        map.put("id", "11");

        SqlMeta sqlMeta = sqlTemplate.render(map);

        System.out.println(sqlMeta);
    }
}
