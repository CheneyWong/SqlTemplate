package club.fullstack.sqltemplate;

import club.fullstack.sqltemplate.script.OgnlCache;
import club.fullstack.sqltemplate.script.SqlFragment;
import club.fullstack.sqltemplate.token.GenericTokenParser;
import club.fullstack.sqltemplate.token.TokenHandler;

/**
 * 
 */
public class SqlTemplate {

    private SqlFragment sqlFragment;
    private Configuration cfg;

    public SqlTemplate(Configuration cfg,SqlFragment sqlFragment) {
        this.sqlFragment = sqlFragment;
        this.cfg = cfg;
    }


    /**
     *
     * @param data
     * @return
     */
    public SqlMeta render(Object data) {

        Context context = new Context(cfg, data);

        sqlFragment.apply(context);

        parseParameter(context);

        return new SqlMeta(context.getSql(), context.getParameter());
    }

    private void parseParameter(final Context context) {

        String sql = context.getSql();

        GenericTokenParser parser1 = new GenericTokenParser("#{", "}",
                new TokenHandler() {

                    public String handleToken(String content) {

                        // 忽略类型
                        int indexG = content.indexOf(",");
                        if (indexG > 0){
                            content = content.substring(0,indexG);
                        }

                        Object value = OgnlCache.getValue(content,
                                context.getBinding());

                        if (value == null) {
                            throw new RuntimeException("Can not found "
                                    + content + " value");
                        }

                        context.addParameter(value);

                        return "?";
                    }
                });

        sql = parser1.parse(sql);


        context.setSql(sql);

    }
}


