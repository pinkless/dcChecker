import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;

/**
 * ClassName: polynomial
 * Package: PACKAGE_NAME
 * Description:
 *
 * @Author: chy
 * @Create: 2024/5/31 - 17:05
 * @Version: v1.0
 */
public class polynomial {
    public static void storePropFile(Properties prop,String fileName){
        try(FileOutputStream out = new FileOutputStream(fileName)){
            prop.store(out,null);
        }
        catch(IOException e){
            e.printStackTrace();
            System.out.println(fileName +"save failed !");
            System.exit(-1);
        }
    }
    public static Element derivative(String PairingParametersFileName, Element[] param,Element b){
        Pairing bp = PairingFactory.getPairing(PairingParametersFileName);
        int ponum = param.length;
        //System.out.println(ponum);
        Element p[] = param;
        Element result[] =new Element[ponum];
        Element resultall=bp.getZr().newOneElement();
        for(int i=0;i<ponum;i++){
            result[i]=bp.getZr().newOneElement();
            for(int j=0;j<ponum-1;j++){
                if(i+j>=ponum){
                    Element temp =p[j+i-ponum].duplicate().add(b);
                    result[i]=result[i].duplicate().mul(temp);
                }
                if(i+j<ponum){
                    Element temp =p[j+i].duplicate().add(b);
                    result[i]=result[i].duplicate().mul(temp);
                }
                //System.out.println(result[i]);
            }
            if(i==0){
                resultall = result[0];
            }
            if(i>0){
                resultall = resultall.duplicate().add(result[i]);
            }
            //System.out.println(resultall+"000");
        }
        //System.out.println(resultall+"000");
        return resultall;
//        System.out.println(resultall);
//        //存储计算结果
//        //把结果写入到文件里
//        Properties Ider_prop = new Properties();
//        // 将g转换为字符串后写入，但文件中显示乱码，为避免乱码采用Base64编码方式
//        Ider_prop.setProperty("accB", Base64.getEncoder().encodeToString(resultall.toBytes()));
//        storePropFile(Ider_prop,resultallFileName);
    }
    public static void poly2(String PairingParametersFileName, Element[] param,Element b){
          Pairing bp = PairingFactory.getPairing(PairingParametersFileName);
          int ponum = param.length;
          Element p[] = param;
          Element a[] = new Element[ponum+1];
          Element result = b;
          for(int i=0;i<ponum;i++){

          }
          a[0]=bp.getZr().newOneElement();
          a[1]=p[0].duplicate().add(p[1]);
          a[2]=p[0].duplicate().mul(p[1]);
    }

    public static void poly3(String PairingParametersFileName, Element[] param){
        Pairing bp = PairingFactory.getPairing(PairingParametersFileName);
        int ponum = param.length;
        Element p[] = param;
        Element a[] = new Element[ponum+1];
        a[0]=bp.getZr().newOneElement();
        for(int i =0;i<ponum;i++){
            //p[i].duplicate().
        }

    }
    public static void main(String[] args) throws Exception{
        String pairingParametersFileName = "a.properties";
        Pairing bp = PairingFactory.getPairing(pairingParametersFileName);
        Element p[] = new Element[3];
        p[0]=bp.getZr().newElement(3).getImmutable();
        p[1]=bp.getZr().newElement(4).getImmutable();
        p[2]=bp.getZr().newElement(5).getImmutable();
        //p[3]=bp.getZr().newElement(6).getImmutable();
        Element bb = bp.getZr().newElement(-2).getImmutable();
        //poly2(pairingParametersFileName,p);
        Element result = derivative(pairingParametersFileName,p,bb);
        //System.out.println(result+"222");
    }
}
