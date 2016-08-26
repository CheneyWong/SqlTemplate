package club.fullstack.sqltemplate.script;

import club.fullstack.sqltemplate.Context;

/**
 * Created by Cheney on 2016/8/23.
 */
public class IncludeFragment implements SqlFragment {

    private String content;

    public IncludeFragment(String content) {
        this.content = content;
    }

    public boolean apply(Context context) {

        context.appendSql(this.content);

        return true;
    }
}
