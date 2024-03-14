package tn.amin.mpro2.hook.all;

import java.util.Arrays;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tn.amin.mpro2.constants.OrcaClassNames;
import tn.amin.mpro2.debug.Logger;
import tn.amin.mpro2.hook.BaseHook;
import tn.amin.mpro2.hook.HookId;
import tn.amin.mpro2.hook.listener.HookListenerResult;
import tn.amin.mpro2.orca.OrcaGateway;
import tn.amin.mpro2.orca.datatype.Mention;
import tn.amin.mpro2.orca.datatype.TextMessage;

public class MessageSentHook extends BaseHook {
    public static final String DISPATCH_METHOD = "dispatchVIJOOOOOOOOOOOOOOOOOOOOOOOOO";

    public MessageSentHook() {
        super();
    }

    @Override
    public HookId getId() {
        return HookId.MESSAGE_SEND;
    }

    @Override
    protected Set<XC_MethodHook.Unhook> injectInternal(OrcaGateway gateway) {
        final Class<?> MailboxCoreJNI = XposedHelpers.findClass(OrcaClassNames.MAILBOX_CORE_JNI, gateway.classLoader);

        return XposedBridge.hookAllMethods(MailboxCoreJNI, DISPATCH_METHOD, wrap(new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                if (param.args[6] instanceof String message) {

                    Long threadKey = (Long) param.args[2];

                    String rangeStartsString = (String) param.args[8];
                    String rangeEndsString = (String) param.args[9];
                    String threadKeysString = (String) param.args[10];
                    String typesString = (String) param.args[11];
                    String replyMessageId = (String) param.args[12];
                    TextMessage originalMessage = new TextMessage.Builder(message)
                            .setMentions(Mention.fromDispatchArgs(message, rangeStartsString, rangeEndsString, threadKeysString, typesString))
                            .setReplyMessageId(replyMessageId)
                            .build();

                    notifyListenersWithResult((listener) -> ((MessageSentListener) listener).onMessageSent(originalMessage, threadKey));

                    if (getListenersReturnValue().isConsumed && getListenersReturnValue().value == null) {
                        param.setResult(null);
                        return;
                    }

                    TextMessage refinedMessage = (TextMessage) getListenersReturnValue().value;
                    if (refinedMessage == null) return;

//                    Logger.logObjectRecursive(refinedMessage);
                    param.args[6] = refinedMessage.content;
                    param.args[8] = Mention.joinRangeStarts(refinedMessage.mentions);
                    param.args[9] = Mention.joinRangeEnds(refinedMessage.mentions);
                    param.args[10] = Mention.joinThreadKeys(refinedMessage.mentions);
                    param.args[11] = Mention.joinTypes(refinedMessage.mentions);
                    param.args[12] = refinedMessage.replyMessageId;
                }
            }
        }));
    }

    public interface MessageSentListener {
        HookListenerResult<TextMessage> onMessageSent(TextMessage message, Long threadKey);
    }
}
