import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

import javax.xml.bind.SchemaOutputResolver;
import java.lang.reflect.Parameter;
import java.util.Queue;

/**
 * ClassName: JPBCDemo
 * Package: PACKAGE_NAME
 * Description:
 *
 * @Author: chy
 * @Create: 2024/4/25 - 16:36
 * @Version: v1.0
 */
public class JPBCDemo {
    public static void main(String[] args) {
        Pairing bp = PairingFactory.getPairing("a.properties");
        //定义群
        Field G1 = bp.getG1();
        Field Zr = bp.getZr();

        //定义群中的元素
        Element g = G1.newRandomElement();
        Element a = Zr.newRandomElement();
        Element b = Zr.newRandomElement();

        Element g_a = g.duplicate().powZn(a);
        Element g_b = g.duplicate().powZn(b);
        Element egg_ab = bp.pairing(g_a,g_b);

        Element egg = bp.pairing(g,g);
        Element ab = a.duplicate().mul(b);
        Element egg_ab_p = egg.duplicate().powZn(ab);

//        if (egg_ab_p.isEqual(egg_ab)) {
//            System.out.println("yes");
//        }else
//            System.out.println("no");


//        Element a_add_b = a.duplicate().add(b);
//        if(ab.isEqual(a_add_b)){
//            System.out.println("yes");
//            System.out.println("yes");
//        }else
//            System.out.println(ab);
//            System.out.println(a_add_b);
//            System.out.println("no");
    }
}
