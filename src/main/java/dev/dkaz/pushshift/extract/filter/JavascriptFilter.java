package dev.dkaz.pushshift.extract.filter;

import dev.dkaz.pushshift.extract.Args;
import dev.dkaz.pushshift.extract.Main;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public class JavascriptFilter implements FilterStrategy {
    public static final FilterFactory FILTER_FACTORY = () -> {
        JavascriptFilter filter = new JavascriptFilter();
        filter.context = Context.newBuilder("js").engine(Main.ENGINE).allowAllAccess(true).build();
        filter.bindings = filter.context.getBindings("js");
        filter.script = filter.context.eval(Args.jsScript);
        return filter;
    };

    private Context context;
    private Value bindings;
    private Value script;

    @Override
    public boolean isAllowed(String line) {
        bindings.putMember("line", line);
        bindings.putMember("allow", Boolean.FALSE);
        script.execute();
        return bindings.getMember("allow").asBoolean();
    }

    @Override
    public void close() {
        if (context != null) context.close();
    }
}
