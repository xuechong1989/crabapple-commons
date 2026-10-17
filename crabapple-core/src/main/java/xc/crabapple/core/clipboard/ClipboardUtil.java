package xc.crabapple.core.clipboard;

import xc.crabapple.core.log.LogUtil;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

/**
 * 2017-05-10 1.0.0
 */
public final class ClipboardUtil{

    /**
     * 2017-05-10 1.0.0
     * @param value
     */
    public static final void setString(final String value){
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        String str = value;
        StringSelection selection = new StringSelection(str);
        clipboard.setContents(selection, null);
    }

    /**
     * 2017-05-10 1.0.0
     */
    public static final String getString(){
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable content = clipboard.getContents(null);
        if(content.isDataFlavorSupported(DataFlavor.stringFlavor)){  //判断是否为文本类型
            try{
                String str = (String)content.getTransferData(DataFlavor.stringFlavor);//从数据中获取文本值
                if(str == null){
                    return null;
                }
                return str;
            }catch(Throwable throwable){
                LogUtil.error("ClipboardUtil_getString_t1", throwable);
            }
        }
        return null;
    }

}
