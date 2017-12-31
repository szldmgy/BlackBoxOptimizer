package utils;

/**
 * Created by peterkiss on 14/05/17.
 */
public class EnumParam extends Param<String> {

    public EnumParam(String value, String upper, String lower, String name) {
        super(value, upper, lower, name);
    }

    public EnumParam(String value, String[] values, String name) {
        super(value, values, name);
    }

    @Override
    public String getParamTypeName(){
        return "Enum";
    }

}
