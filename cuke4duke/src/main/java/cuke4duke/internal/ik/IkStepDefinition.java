package cuke4duke.internal.ik;

import cuke4duke.Table;
import cuke4duke.PyString;
import cuke4duke.internal.JRuby;
import cuke4duke.internal.Utils;
import cuke4duke.internal.language.AbstractStepDefinition;
import cuke4duke.internal.language.StepArgument;
import ioke.lang.IokeObject;
import ioke.lang.Message;
import ioke.lang.Runtime;
import ioke.lang.exceptions.ControlFlow;

import java.util.ArrayList;
import java.util.List;

public class IkStepDefinition extends AbstractStepDefinition {
    private final Runtime ioke;
    private final IokeObject iokeStepDefObject;
    private String regexpSource;

	// Not used yet
    public static void throwCucumberIokeException(String message) {
        throw JRuby.error("IokeException", message);
    }

    public IkStepDefinition(IkLanguage ikLanguage, Runtime ioke, IokeObject iokeStepDefObject) throws Throwable {
        super(ikLanguage);
        this.ioke = ioke;
        this.iokeStepDefObject = iokeStepDefObject;
        register();
    }

    protected Class<?>[] getParameterTypes(Object[] args) {
        return Utils.objectClassArray(args.length);
    }

    public void invokeWithJavaArgs(Object[] args) throws Throwable {
        IokeObject msg = ioke.newMessage("invoke");
        Message invoke = (Message) IokeObject.data(msg);

        // TODO: catch expectation failures and resignal correctly
        List<Runtime.RescueInfo> pendingRescues = new ArrayList<Runtime.RescueInfo>();
        IokeObject rr = IokeObject.as(((Message)IokeObject.data(ioke.mimic)).sendTo(ioke.mimic, ioke.ground, ioke.rescue), ioke.ground);
        List<Object> conds = new ArrayList();
        final IokeObject pendingCondition = IokeObject.as(IokeObject.getCellChain(ioke.condition,
                                                                                  ioke.message,
                                                                                  ioke.ground,
                                                                                  "Pending"), ioke.ground);
        conds.add(pendingCondition);
        pendingRescues.add(new Runtime.RescueInfo(rr, conds, pendingRescues, ioke.getBindIndex()));
        ioke.registerRescues(pendingRescues);
        try {
	        invoke.sendTo(msg, iokeStepDefObject, iokeStepDefObject, multilineArg(args));
        } catch(ControlFlow.Rescue e) {
            if(e.getRescue().token == pendingRescues) {
                throw JRuby.cucumberPending("TODO");
                //                throw (Exception)(e.getCondition().getCell(ioke.message, ioke.ground, "rootException"));
            } else {
                throw e;
            }
        } finally {
            ioke.unregisterRescues(pendingRescues);
        }
    }

    public String regexp_source() throws Throwable {
        if(regexpSource == null) findRegexpSource();
        return regexpSource;
    }

    public String file_colon_line() throws Throwable {
        return regexp_source();
    }

    public List<StepArgument> arguments_from(String stepName) throws Throwable {
        IokeObject msg = ioke.newMessage("arguments_from");
        Message arguments_from = (Message) IokeObject.data(msg);
        Object args = arguments_from.sendTo(msg, iokeStepDefObject, iokeStepDefObject, stepName);
        if(args instanceof List) {
            return (List<StepArgument>) args;
        } else {
            return null;
        }
    }

    private void findRegexpSource() throws ControlFlow {
        IokeObject msg = ioke.newMessage("regexp_source");
        Message regexp_source = (Message) IokeObject.data(msg);
        regexpSource = regexp_source.sendTo(msg, iokeStepDefObject, iokeStepDefObject).toString();
    }

    private Object multilineArg(Object[] args) {
        Object multilineArg;
        if(args.length > 0) {
            if(args[args.length-1] instanceof PyString) {
                multilineArg = ioke.newText(((PyString) args[args.length-1]).to_s());
            } else if(args[args.length-1] instanceof Table) {
                multilineArg = args[args.length-1];
            } else {
                multilineArg = ioke.nil;
            }
        } else {
            multilineArg = ioke.nil;
        }
        return multilineArg;
    }

}