import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

import java.io.*;
import java.util.Base64;
import java.util.Properties;

/**
 * ClassName: Provider
 * Package: PACKAGE_NAME
 * Description:
 *
 * @Author: chy
 * @Create: 2024/5/9 - 21:24
 * @Version: v1.0
 */
public class Provider {
    public static void witnessGen(int chall,int chunkNum,int chunkSize,String chunkPath,String PairingParametersFileName,String sFileName,String gFileName,String witFileName){
        Pairing bp = PairingFactory.getPairing(PairingParametersFileName);
        Element m[] = new Element[chunkNum];
        //读取s
        Properties s_prop =new Properties();
        try {
            s_prop.load(new FileInputStream(sFileName));
        }catch(IOException ex){
            ex.printStackTrace();
        }
        String sString = s_prop.getProperty("s");
        Element s = bp.getZr().newElementFromBytes(Base64.getDecoder().decode(sString)).getImmutable();

        //读取g
        Properties g_prop =new Properties();
        try{
            g_prop.load(new FileInputStream(gFileName));
        }catch(IOException ex){
            ex.printStackTrace();
        }
        String gString = g_prop.getProperty("g");
        Element g = bp.getG1().newElementFromBytes(Base64.getDecoder().decode(gString)).getImmutable();

        byte[] b_chall = null;
        //生成证明
        for(int i = 1;i<=chunkNum;i++){
            //设置缓冲区
            byte[] bytes = new byte[chunkSize];
            //创建分块文件
            File file = new File(chunkPath + i);
            if(!file.exists()){
                continue;
            }
            try {
                //向分块文件中写数据
                BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
                byte[] tmp = null;
                int len = -1;
                while((len = fis.read(bytes)) != -1){
                    tmp = bytes;
                    //System.out.println(tmp);
                    ShaUtil su = new ShaUtil();  //哈希函数类的对象
                    byte[] tmphash = su.getSHA1(tmp,true);
                    m[i-1] = bp.getZr().newElementFromHash(tmphash,0,tmphash.length);
                    //System.out.println(m[i-1]);
                    if(i==chall){
                        b_chall = tmphash;
                    }
                }
                fis.close();
            }catch(IOException ex){
                ex.printStackTrace();
            }
        }
//删除某些数据块后出错
//        Element mm = m[0].duplicate().add(s);
//        Element witness = g.duplicate().powZn(mm);
//        for(int i =0;i<=chunkNum-1;i++)
//        {
//            Element b = m[i].duplicate().add(s);
//            if(i>0&&i!=chall-1){
//                witness = witness.duplicate().powZn(b);
//            }
//            //System.out.println(i);
//        }

        //        long startTime = System.currentTimeMillis();  //测试算法执行时间的开始时间
        int flag =0;
        for(int i =0;i<=chunkNum-1;i++){
            if(m[i]!=null){
                flag = i;
                break;
            }
        }
//        long endTime = System.currentTimeMillis();
//        System.out.println(endTime-startTime);
        //System.out.println(flag);
        Element mm = m[flag].duplicate().add(s);
        Element witness = g.duplicate().powZn(mm);
        for(int i =0;i<=chunkNum-1;i++)
        {
            if(m[i]==null){
                continue;
            }
            if(i>flag){
                //System.out.println(i);
                Element b = m[i].duplicate().add(s);
                if(i!=chall-1){
                    witness = witness.duplicate().powZn(b);
                }
            }
            //System.out.println(i);
        }

        Auditor a = new Auditor();
        //把witness写入到文件里
        Properties witness_prop = new Properties();
        // 将g转换为字符串后写入，但文件中显示乱码，为避免乱码采用Base64编码方式
        witness_prop.setProperty("b_chall", Base64.getEncoder().encodeToString(m[chall-1].toBytes()));
        witness_prop.setProperty("witness", Base64.getEncoder().encodeToString(witness.toBytes()));
        a.storePropFile(witness_prop,witFileName);

    }

    public static void allWitnessGen(int providernum,int chall,int chunkNum,int averagenum,int blocksize,String partwitFileName,String chunkPath,String paramfileName,String pairingParametersFileName,String sFileName,String gFileName){
        long startTime = System.currentTimeMillis();  //测试算法执行时间的开始时间
        witnesspartGen(providernum,chall,chunkNum,averagenum,blocksize,chunkPath,pairingParametersFileName,sFileName,gFileName,paramfileName,partwitFileName);
        long endTime = System.currentTimeMillis();
        System.out.println(endTime-startTime);
    }
    public static void witnesspartGen(int providernum,int chall,int chunkNum,int averagenum,int chunkSize,String chunkPath,String PairingParametersFileName,String sFileName,String gFileName,String paramfileName,String witFileName){
        Pairing bp = PairingFactory.getPairing(PairingParametersFileName);
        Element m[] = new Element[chunkNum];
        //读取s
        Properties s_prop =new Properties();
        try {
            s_prop.load(new FileInputStream(sFileName));
        }catch(IOException ex){
            ex.printStackTrace();
        }
        String sString = s_prop.getProperty("s");
        Element s = bp.getZr().newElementFromBytes(Base64.getDecoder().decode(sString)).getImmutable();

        //读取param
        Properties param_prop =new Properties();
        try {
            param_prop.load(new FileInputStream(paramfileName));
        }catch(IOException ex){
            ex.printStackTrace();
        }
        String paramString = param_prop.getProperty("param"+providernum);
        Element param = bp.getZr().newElementFromBytes(Base64.getDecoder().decode(paramString)).getImmutable();
        int averagechunknum =averagenum;
        //读取g
        Properties g_prop =new Properties();
        try{
            g_prop.load(new FileInputStream(gFileName));
        }catch(IOException ex){
            ex.printStackTrace();
        }
        String gString = g_prop.getProperty("g");
        Element g = bp.getG1().newElementFromBytes(Base64.getDecoder().decode(gString)).getImmutable();

        byte[] b_chall = null;
        //生成证明
        for(int i = 1;i<=chunkNum;i++){
            //设置缓冲区
            byte[] bytes = new byte[chunkSize];
            //创建分块文件
            File file = new File(chunkPath + (i+(providernum-1)*averagechunknum));
            if(!file.exists()){
                continue;
            }
            try {
                //向分块文件中写数据
                BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
                byte[] tmp = null;
                int len = -1;
                while((len = fis.read(bytes)) != -1){
                    tmp = bytes;
                    //System.out.println(tmp);
                    ShaUtil su = new ShaUtil();  //哈希函数类的对象
                    byte[] tmphash = su.getSHA1(tmp,true);
                    m[i-1] = bp.getZr().newElementFromHash(tmphash,0,tmphash.length);
                    //System.out.println(m[i-1]);
                    if(i==chall){
                        b_chall = tmphash;
                    }
                }
                fis.close();
            }catch(IOException ex){
                ex.printStackTrace();
            }
        }

//        long startTime = System.currentTimeMillis();  //测试算法执行时间的开始时间
        int flag =0;
        for(int i =0;i<=chunkNum-1;i++){
            if(m[i]!=null){
                flag = i;
                break;
            }
        }
//        long endTime = System.currentTimeMillis();
//        System.out.println(endTime-startTime);
        //System.out.println(flag);
        Element mm = m[flag].duplicate().add(s);
        Element witness = g.duplicate().powZn(mm);
        for(int i =0;i<=chunkNum-1;i++)
        {
            if(m[i]==null){
                continue;
            }
            if(i>flag){
                //System.out.println(i);
                Element b = m[i].duplicate().add(s);
                if(i!=chall-1){
                    witness = witness.duplicate().powZn(b);
                }
            }
            //System.out.println(i);
        }
        witness = witness.duplicate().powZn(param);
        Auditor a = new Auditor();
        //把witness写入到文件里
        Properties witness_prop = new Properties();
        // 将g转换为字符串后写入，但文件中显示乱码，为避免乱码采用Base64编码方式
        witness_prop.setProperty("b_chall", Base64.getEncoder().encodeToString(m[chall-1].toBytes()));
        witness_prop.setProperty("witness", Base64.getEncoder().encodeToString(witness.toBytes()));
        a.storePropFile(witness_prop,witFileName);
    }
    public static void main(String[] args) throws Exception{
        String Filepath = "D:\\Code\\IDEA\\Decentralized_storage_auditor\\Data\\date\\60mb.doc";
        String dir = "data/";
        String pairingParametersFileName = "a.properties";
        String gFileName = dir + "g.properties";
        String sFileName = dir + "s.properties";
        String accFileName = dir + "acc.properties";
        String witFileName = "StorageProvider/" + "witness.properties";
          //D为1
//        int chall = 16;
//        int chunkNum =512;
//        int blocksize =4*1024;
//        String chunkPath = dir + "datachunk/";
//        long startTime = System.currentTimeMillis();  //测试算法执行时间的开始时间
//        witnessGen(chall,chunkNum,blocksize,chunkPath,pairingParametersFileName,sFileName,gFileName,witFileName);
//        long endTime = System.currentTimeMillis();
//        System.out.println(endTime-startTime);

          //D为2
//        int providernum = 1;
//        int chall = 66;
//        int chunkNum = 2178;
//        int averagenum = 2178;
//        int blocksize =256;
//        String partwitFileName = "StorageProvider/" + "witness1.properties";
//        String chunkPath = "StorageProvider/" + "datachunk1/";
//        int providernum = 2;
//        int chall = 66;
//        int chunkNum = 2179;
//        int averagenum = 2178;
//        int blocksize =256;
//        String partwitFileName = "StorageProvider/" + "witness2.properties";
//        String chunkPath = "StorageProvider/" + "datachunk2/";

        //D为3
//        int providernum = 1;
//        int chall = 66;
//        int chunkNum = 1452;
//        int averagenum = 1452;
//        int blocksize =256;
//        String partwitFileName = "StorageProvider/" + "witness1.properties";
//        String chunkPath = "StorageProvider/" + "datachunk1/";
//        int providernum = 2;
//        int chall = 66;
//        int chunkNum = 1452;
//        int averagenum = 1452;
//        int blocksize =256;
//        String partwitFileName = "StorageProvider/" + "witness2.properties";
//        String chunkPath = "StorageProvider/" + "datachunk2/";
//        int providernum = 3;
//        int chall = 66;
//        int chunkNum = 1453;
//        int averagenum = 1452;
//        int blocksize =256;
//        String partwitFileName = "StorageProvider/" + "witness3.properties";
//        String chunkPath = "StorageProvider/" + "datachunk3/";

        //D为4
//        int providernum = 1;
//        int chall = 66;
//        int chunkNum = 1089;
//        int averagenum = 1089;
//        int blocksize =256;
//        String partwitFileName = "StorageProvider/" + "witness1.properties";
//        String chunkPath = "StorageProvider/" + "datachunk1/";
//        int providernum = 2;
//        int chall = 66;
//        int chunkNum = 1089;
//        int averagenum = 1089;
//        int blocksize =256;
//        String partwitFileName = "StorageProvider/" + "witness2.properties";
//        String chunkPath = "StorageProvider/" + "datachunk2/";
//        int providernum = 3;
//        int chall = 66;
//        int chunkNum = 1089;
//        int averagenum = 1089;
//        int blocksize =256;
//        String partwitFileName = "StorageProvider/" + "witness3.properties";
//        String chunkPath = "StorageProvider/" + "datachunk3/";
//        int providernum = 4;
//        int chall = 66;
//        int chunkNum = 1090;
//        int averagenum = 1089;
//        int blocksize =256;
//        String partwitFileName = "StorageProvider/" + "witness4.properties";
//        String chunkPath = "StorageProvider/" + "datachunk4/";

        //D为5
//        int providernum = 1;
//        int chall = 66;
//        int chunkNum = 3072;
//        int averagenum = 3072;
//        int blocksize =4*1024;
//        String partwitFileName = "StorageProvider/" + "witness1.properties";
//        String chunkPath = "StorageProvider/" + "datachunk1/";
//        int providernum = 2;
//        int chall = 66;
//        int chunkNum = 3072;
//        int averagenum = 3072;
//        int blocksize =4*1024;
//        String partwitFileName = "StorageProvider/" + "witness2.properties";
//        String chunkPath = "StorageProvider/" + "datachunk2/";
//        int providernum = 3;
//        int chall = 66;
//        int chunkNum = 3072;
//        int averagenum = 3072;
//        int blocksize =4*1024;
//        String partwitFileName = "StorageProvider/" + "witness3.properties";
//        String chunkPath = "StorageProvider/" + "datachunk3/";
//        int providernum = 4;
//        int chall = 66;
//        int chunkNum = 3072;
//        int averagenum = 3072;
//        int blocksize =4*1024;
//        String partwitFileName = "StorageProvider/" + "witness4.properties";
//        String chunkPath = "StorageProvider/" + "datachunk4/";
//        int providernum = 5;
//        int chall = 66;
//        int chunkNum = 3072;
//        int averagenum = 3072;
//        int blocksize =4*1024;
//        String partwitFileName = "StorageProvider/" + "witness5.properties";
//        String chunkPath = "StorageProvider/" + "datachunk5/";
//

        //D为10  由于每个D存储的文件块一样多，用循环生成每个D的证明信息
        int providernum = 3;
        int chall = 11;
        int chunkNum = 128;

        int averagenum = 128;
        int blocksize =4*1024;
        String paramfileName = "StorageProvider/" +"param.properties";
        for(int i=1;i<=20;i++){
            String partwitFileName = "StorageProvider/" + "witness"+i+".properties";
            String chunkPath = "StorageProvider/" + "datachunk"+i+"/";
            allWitnessGen(i,chall,chunkNum,averagenum,blocksize,partwitFileName,chunkPath,paramfileName,pairingParametersFileName,sFileName,gFileName);
        }


//        String paramfileName = "StorageProvider/" +"param.properties";
//        long startTime = System.currentTimeMillis();  //测试算法执行时间的开始时间
//        witnesspartGen(providernum,chall,chunkNum,averagenum,blocksize,chunkPath,pairingParametersFileName,sFileName,gFileName,paramfileName,partwitFileName);
//        long endTime = System.currentTimeMillis();
//        System.out.println(endTime-startTime);
    }
}
