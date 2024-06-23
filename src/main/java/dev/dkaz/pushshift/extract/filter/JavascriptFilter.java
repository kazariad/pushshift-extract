package dev.dkaz.pushshift.extract.filter;

import dev.dkaz.pushshift.extract.Args;
import dev.dkaz.pushshift.extract.Main;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

public class JavascriptFilter implements FilterStrategy {
    public static final FilterFactory FILTER_FACTORY = () -> {
        JavascriptFilter jf = new JavascriptFilter();
        jf.context = Context.newBuilder("js").engine(Main.ENGINE).allowAllAccess(true).build();
        jf.context.eval(Args.jsSource);

        Source conversionWrapper = Source.create("js", """
                function __filterW(line) {
                    return true == filter(line);
                }
                """);

        jf.context.eval(conversionWrapper);
        jf.filterFunc = jf.context.getBindings("js").getMember("__filterW");
        return jf;
    };

    private Context context;
    private Value filterFunc;

    @Override
    public boolean isAllowed(String line) {
        Value isAllowed = filterFunc.execute(line);
        return isAllowed.asBoolean();
    }

    @Override
    public void close() {
        if (context != null) context.close();
    }
}
