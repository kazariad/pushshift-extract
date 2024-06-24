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
                function __myFilter(line) {
                    return true == myFilter(line);
                }
                """);

        jf.context.eval(conversionWrapper);
        jf.myFilterFunc = jf.context.getBindings("js").getMember("__myFilter");
        return jf;
    };

    private Context context;
    private Value myFilterFunc;

    @Override
    public boolean isAllowed(String line) {
        Value isAllowed = myFilterFunc.execute(line);
        return isAllowed.asBoolean();
    }

    @Override
    public void close() {
        if (context != null) context.close();
    }
}
