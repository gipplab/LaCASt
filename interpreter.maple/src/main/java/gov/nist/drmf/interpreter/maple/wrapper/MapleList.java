package gov.nist.drmf.interpreter.maple.wrapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Andre Greiner-Petter
 */
public class MapleList extends Algebraic implements IMapleInstance {
    private static final String CLAZZ_NAME = "List";
    private static final String CLAZZ_PATH = "com.maplesoft.openmaple." + CLAZZ_NAME;

    private final com.maplesoft.openmaple.List mList;

    MapleList(com.maplesoft.openmaple.List mList) {
        super(mList);
        this.mList = mList;
    }

    public int length() throws MapleException {
        try {
            return mList.length();
        } catch (Exception e) {
            throw new MapleException(e);
        }
    }

    public Algebraic get(int i) {
        return OpenMapleWrapperHelper.delegateOpenMapleObject(mList.get(i));
    }

    public Algebraic select(int i) throws MapleException {
        try {
            return OpenMapleWrapperHelper.delegateOpenMapleObject(mList.select(i));
        } catch (com.maplesoft.externalcall.MapleException e) {
            throw new MapleException(e);
        }
    }

    public List<Algebraic> subList(int fromIndex, int toIndex) {
        List<?> sublist = mList.subList(fromIndex, toIndex);
        return sublist
                .stream()
                .map(OpenMapleWrapperHelper::delegateOpenMapleObject)
                .collect(Collectors.toList());
    }

    @Override
    public String getClassPath() {
        return CLAZZ_PATH;
    }

    public static boolean isInstance(Object obj) {
        if ( obj instanceof MapleList ) return true;
        return OpenMapleWrapperHelper.isInstance(obj, CLAZZ_PATH);
    }

    public static MapleList cast(Object obj) throws ClassCastException {
        if ( obj instanceof MapleList ) return (MapleList) obj;
        return new MapleList((com.maplesoft.openmaple.List) obj);
    }
}
