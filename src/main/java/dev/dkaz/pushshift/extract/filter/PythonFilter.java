package dev.dkaz.pushshift.extract.filter;

import dev.dkaz.pushshift.extract.Args;
import dev.dkaz.pushshift.extract.Main;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

public class PythonFilter implements FilterStrategy {
    public static final FilterFactory FILTER_FACTORY = () -> {
        PythonFilter pf = new PythonFilter();
        pf.context = Context.newBuilder("python").engine(Main.ENGINE).allowAllAccess(true).build();
        pf.context.eval(Args.pySource);

        Source conversionWrapper = Source.create("python", """
                def __filterW(line):
                    return bool(filter(line))
                """);

        pf.context.eval(conversionWrapper);
        pf.filterFunc = pf.context.getBindings("python").getMember("__filterW");
        return pf;
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
