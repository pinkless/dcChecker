import com.sun.org.apache.xpath.internal.operations.Minus;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import sun.security.provider.SHA;
import sun.security.rsa.RSASignature;

import java.io.*;
import java.util.*;

/**
 * ClassName: Auditor
 * Package: PACKAGE_NAME
 * Description:
 *
 * @Author: chy
 * @Create: 2024/5/8 - 16:48
 * @Version: v1.0
 */
public class Auditor {
    public static void setup(String PairingParametersFileName,String gfileName,String sfileName){
        Pairing bp = PairingFactory.getPairing(PairingParametersFileName);

        Element g = bp.getG1().newRandomElement().getImmutable();
        Element s = bp.getZr().newRandomElement().getImmutable();
        Properties g_prop = new Properties();
        // 将g转换为字符串后写入，但文件中显示乱码，为避免乱码采用Base64编码方式
        g_prop.setProperty("g", Base64.getEncoder().encodeToString(g.toBytes()));
        storePropFile(g_prop,gfileName);

        Properties s_prop = new Properties();
        // 将g转换为字符串后写入，但文件中显示乱码，为避免乱码采用Base64编码方式
        s_prop.setProperty("s", Base64.getEncoder().encodeToString(s.toBytes()));
        storePropFile(s_prop,sfileName);
    }
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

    public static void accGen(String Filepath,String pairingParametersFileName,int blocksize,String sFileName,String gFileName,String accFileName){
        File f = new File(Filepath);

        Pairing bp = PairingFactory.getPairing(pairingParametersFileName);
        Element m[] = new Element[60000];  //假定最大不超过60000块
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
//        System.out.println(Base64.getDecoder().decode(gString).length);
        Element g = bp.getG1().newElementFromBytes(Base64.getDecoder().decode(gString)).getImmutable();

        try(InputStream inputStream = new FileInputStream(Filepath)){
            byte[] tmp = null;
            StringBuilder stringBuilder = new StringBuilder();
            int i = 0;
            while(true){
                byte[] buffer = new byte[blocksize];
                int len = inputStream.read(buffer);
                if(len == -1){
                    break;
                }
                tmp = buffer;
                ShaUtil su = new ShaUtil();  //哈希函数类的对象
                byte[] tmphash = su.getSHA1(tmp,true);
//                System.out.println(tmphash.length);
                m[i] = bp.getZr().newElementFromHash(tmphash,0,tmphash.length);
                //System.out.println(m[i]);
                i++;
                stringBuilder.append(tmp);
            }
        }catch(IOException e){
            e.printStackTrace();
        };

        Element mm = m[0].duplicate().add(s);
        Element accB = g.duplicate().powZn(mm);
        for(int i =0;m[i]!=null;i++)
        {
            Element b = m[i].duplicate().add(s);
            if(i>0){
                accB = accB.duplicate().powZn(b);
            }
        }
        //把acc写入到文件里
        Properties acc_prop = new Properties();
        // 将g转换为字符串后写入，但文件中显示乱码，为避免乱码采用Base64编码方式
        acc_prop.setProperty("accB", Base64.getEncoder().encodeToString(accB.toBytes()));
        storePropFile(acc_prop,accFileName);
    }

    public static void paramGen(String PairingParametersFileName,String Filepath,int Provider_num,int blocksize,String sFileName,String chunkpath,String paramfileName){
        Pairing bp = PairingFactory.getPairing(PairingParametersFileName);
        File sourceFile = new File(Filepath);
        File providerChunk = new File(chunkpath);
        int chunksize = blocksize;
        int []prochunknum = new int[Provider_num];
        int []prochunkpram = new int[Provider_num];
        //分块数量
        int chunkNum = (int)Math.ceil(sourceFile.length()*1.0/chunksize);
        int averagechunknum = chunkNum/Provider_num;

//        System.out.println(chunkNum);
//        System.out.println(averagechunknum);
        if(chunkNum<=0){
            chunkNum=1;
        }
        int i = 0;
        int chunkNumcp=chunkNum;
        while(chunkNumcp>=averagechunknum){
            if(chunkNumcp>=averagechunknum&&(chunkNumcp-averagechunknum)>=averagechunknum){
                prochunknum[i] = averagechunknum;
            }
            else{
                prochunknum[i] = chunkNumcp;
            }
            chunkNumcp=chunkNumcp-averagechunknum;
            i++;
        }
        for(int j=0;j<Provider_num;j++){
            System.out.println(prochunknum[j]);
        }

        //读取s
        Properties s_prop =new Properties();
        try {
            s_prop.load(new FileInputStream(sFileName));
        }catch(IOException ex){
            ex.printStackTrace();
        }
        String sString = s_prop.getProperty("s");
        Element s = bp.getZr().newElementFromBytes(Base64.getDecoder().decode(sString)).getImmutable();

        //生成证明
        Properties param_prop = new Properties();
        for(int n=0;n<prochunknum.length;n++){
            Element m[] = new Element[chunkNum];
            int mi = 0;
            for(int j = 1;j<=chunkNum;j++){
                if((j<=n*averagechunknum||j>(n+1)*averagechunknum)&&(chunkNum-(n+1)*averagechunknum)>=averagechunknum){
                    //System.out.println(j);
                    //设置缓冲区
                    byte[] bytes = new byte[blocksize];
                    //创建分块文件
                    File file = new File(chunkpath + j);
                    try {
                        //向分块文件中写数据
                        BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
                        byte[] tmp = null;
                        int len = -1;
                        while((len = fis.read(bytes)) != -1){
                            tmp = bytes;
                            ShaUtil su = new ShaUtil();  //哈希函数类的对象
                            byte[] tmphash = su.getSHA1(tmp,true);
                            m[mi] = bp.getZr().newElementFromHash(tmphash,0,tmphash.length);
                            //System.out.println(m[mi]);
                        }
                        fis.close();
                    }catch(IOException ex){
                        ex.printStackTrace();
                    }
                    mi++;
                }
                if(j<=n*averagechunknum&&(chunkNum-(n+1)*averagechunknum)<averagechunknum){
                    //System.out.println("2if"+j);
                    //设置缓冲区
                    byte[] bytes = new byte[blocksize];
                    //创建分块文件
                    File file = new File(chunkpath + j);
                    try {
                        //向分块文件中写数据
                        BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
                        byte[] tmp = null;
                        int len = -1;
                        while((len = fis.read(bytes)) != -1){
                            tmp = bytes;
                            ShaUtil su = new ShaUtil();  //哈希函数类的对象
                            byte[] tmphash = su.getSHA1(tmp,true);
                            m[mi] = bp.getZr().newElementFromHash(tmphash,0,tmphash.length);
                        }
                        fis.close();
                    }catch(IOException ex){
                        ex.printStackTrace();
                    }
                    mi++;
                }            }

            Element param= m[0].duplicate().add(s);
            for(int j =1;j<=(chunkNum-prochunknum[n])-1;j++)
            {
                //System.out.println(j);
                Element b = m[j].duplicate().add(s);
                param = param.duplicate().mul(b);

            }
            // 将g转换为字符串后写入，但文件中显示乱码，为避免乱码采用Base64编码方式
            param_prop.setProperty("param"+(n+1), Base64.getEncoder().encodeToString(param.toBytes()));
            storePropFile(param_prop,paramfileName);
        }
    }

    public static void witparamGen(String PairingParametersFileName,String Filepath,int Provider_num,int blocksize,String sFileName,String gFileName,String witparamfileName) {
        Pairing bp = PairingFactory.getPairing(PairingParametersFileName);
        File sourceFile = new File(Filepath);
        int chunksize = blocksize;
        //分块数量
        int chunkNum = (int) Math.ceil(sourceFile.length() * 1.0 / chunksize);
//        System.out.println(chunkNum);
        //读取s
        Properties s_prop = new Properties();
        try {
            s_prop.load(new FileInputStream(sFileName));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        String sString = s_prop.getProperty("s");
        Element s = bp.getZr().newElementFromBytes(Base64.getDecoder().decode(sString)).getImmutable();

        //读取g
        Properties g_prop = new Properties();
        try {
            g_prop.load(new FileInputStream(gFileName));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        String gString = g_prop.getProperty("g");
        Element g = bp.getG1().newElementFromBytes(Base64.getDecoder().decode(gString)).getImmutable();

        Properties witparam_prop = new Properties();
        Element s_exponent = bp.getZr().newOneElement();
        if (Provider_num > 0) {
            chunkNum = (int) Math.ceil(chunkNum / Provider_num);
            witparam_prop.setProperty("witparam" + 0, Base64.getEncoder().encodeToString(g.toBytes()));
            storePropFile(witparam_prop, witparamfileName);
            for (int i = 1; i <= chunkNum; i++) {
                for (int j = 1; j <= i; j++) {
                    s_exponent = s_exponent.getImmutable().mul(s);
                }
                Element g_s_ex = g.getImmutable().powZn(s_exponent);
                // 将g转换为字符串后写入，但文件中显示乱码，为避免乱码采用Base64编码方式
                witparam_prop.setProperty("witparam" + i, Base64.getEncoder().encodeToString(g_s_ex.toBytes()));
                storePropFile(witparam_prop, witparamfileName);
            }
        }
        if (Provider_num == 0) {
            witparam_prop.setProperty("witparam" + 0, Base64.getEncoder().encodeToString(g.toBytes()));
            storePropFile(witparam_prop, witparamfileName);
            for (int i = 1; i <= chunkNum; i++) {
                for (int j = 1; j <= i; j++) {
                    s_exponent = s_exponent.getImmutable().mul(s);
                }
                Element g_s_ex = g.getImmutable().powZn(s_exponent);
                // 将g转换为字符串后写入，但文件中显示乱码，为避免乱码采用Base64编码方式
                witparam_prop.setProperty("witparam" + i, Base64.getEncoder().encodeToString(g_s_ex.toBytes()));
                storePropFile(witparam_prop, witparamfileName);
            }
        }
    }

    public static void Split_Data(String Filepath,int blocksize,String chunkPath){
            File sourceFile = new File(Filepath);
            int chunksize = blocksize;
            String chunkpath = chunkPath;
            File chunkFolder = new File(chunkPath);
            if(!chunkFolder.exists()){
                chunkFolder.mkdirs();
            }
            //分块数量
            long chunkNum = (long)Math.ceil(sourceFile.length()*1.0/chunksize);
            if(chunkNum<=0){
                chunkNum=1;
            }
            //设置缓冲区
            byte[] bytes = new byte[blocksize];
            try {
                BufferedInputStream fis = new BufferedInputStream(new FileInputStream(sourceFile));
                //开始分块
                for(int i = 1;i<=chunkNum;i++){
                    //创建分块文件
                    File file = new File(chunkPath + i);
                    if(file.createNewFile()){
                        //向分块文件中写数据
                        BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(file));
                        int len = -1;
                        int j = 0;
                        while((len = fis.read(bytes)) != -1){
                            fos.write(bytes,0,len);
                            j++;
                            if((j*len)>=chunksize){
                                break;
                            }
                        }
                        fos.close();
                    }
                }
                fis.close();
            }catch(IOException e){
                e.printStackTrace();
            };


    }

    public static void Merge_Data(String chunkPath,String mergePath){
        //需要合并的文件所在的文件夹
        File chunkFolder = new File(chunkPath);
        //合并后的文件
        File mergeFile = new File(mergePath);
        if(mergeFile.exists()){
            mergeFile.delete();
        }
        try {
            //创建合并后的文件
            mergeFile.createNewFile();
            BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(mergeFile));
            //设置缓冲区
            byte[] bytes = new byte[1024];
            //获取分块列表
            File[] fileArray = chunkFolder.listFiles();
            //文件转成集合并排序
            ArrayList<File>fileList = new ArrayList<>(Arrays.asList(fileArray));
            //从小到大进行排序
            Collections.sort(fileList, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    if (Integer.parseInt(o1.getName())<Integer.parseInt(o2.getName())){
                        return -1;
                    }
                        return 1;
                }
            });
            //合并文件
            for (File chunkFile:fileList){
                BufferedInputStream fis = new BufferedInputStream(new FileInputStream(chunkFile));
                int len = -1;
                while ((len=fis.read(bytes)) != -1){
                    fos.write(bytes,0,len); //写入数据
                }
                fis.close();
            }
            fos.close();
        }catch(IOException e){
            e.printStackTrace();
        };

    }

    public static Element derivative_old(String PairingParametersFileName, Element[] param,Element b){
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
                    Element temp =p[j+i-ponum].duplicate().sub(b);
                    result[i]=result[i].duplicate().mul(temp);
                }
                if(i+j<ponum){
                    Element temp =p[j+i].duplicate().sub(b);
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

    public static Element derivative(String PairingParametersFileName, Element[] param, Element b) {
        Pairing bp = PairingFactory.getPairing(PairingParametersFileName);
        int ponum = param.length;

        // 计算 (-b + param[i])，即求所有多项式因子的值
        Element[] terms = new Element[ponum];
        for (int i = 0; i < ponum; i++) {
            terms[i] = param[i].duplicate().sub(b);
        }

        // 前缀积和后缀积
        Element[] prefix = new Element[ponum];
        Element[] suffix = new Element[ponum];

        // 初始化前缀积
        prefix[0] = terms[0].duplicate();
        for (int i = 1; i < ponum; i++) {
            prefix[i] = prefix[i - 1].duplicate().mul(terms[i]);
        }

        // 初始化后缀积
        suffix[ponum - 1] = terms[ponum - 1].duplicate();
        for (int i = ponum - 2; i >= 0; i--) {
            suffix[i] = suffix[i + 1].duplicate().mul(terms[i]);
        }

        // 计算导数值
        Element result = bp.getZr().newZeroElement();
        for (int i = 0; i < ponum; i++) {
            Element termProduct = bp.getZr().newOneElement();

            if (i > 0) {
                termProduct.mul(prefix[i - 1]); // 前缀积
            }
            if (i < ponum - 1) {
                termProduct.mul(suffix[i + 1]); // 后缀积
            }

            result.add(termProduct); // 累加所有的导数项
        }

        return result;
    }


    public static void aggregation_witness(int pronum,String pairingParametersFileName,String sFileName,String gFileName,String witFileName){
        Pairing bp = PairingFactory.getPairing(pairingParametersFileName);
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

        //读取witness
        Element chall[]=new Element[pronum];
        Element witness[]=new Element[pronum];
        Properties witness_prop =new Properties();
        for(int i=0;i<pronum;i++){
            try{
                witness_prop.load(new FileInputStream(witFileName+"witness"+(i+1)+".properties"));
                //System.out.println(witFileName+"witness"+i+".properties");
            }catch(IOException ex){
                ex.printStackTrace();
            }
            String bchalString = witness_prop.getProperty("b_chall");
            Element b_chall = bp.getZr().newElementFromBytes(Base64.getDecoder().decode(bchalString)).getImmutable();
            String witnessString = witness_prop.getProperty("witness");
            Element wit = bp.getG1().newElementFromBytes(Base64.getDecoder().decode(witnessString)).getImmutable();
            chall[i]=b_chall;
            //System.out.println(chall[i]);
            witness[i]=wit;
            //System.out.println(witness[i]);
        }
        Element challI=bp.getZr().newOneElement();
        Element witI=bp.getG1().newOneElement();
        Element wit_c[]=new Element[pronum];

        for(int i=0;i<pronum;i++){

//            Element c = bp.getZr().newOneElement(); // 提前初始化，循环中复用
//            for (int j = 0; j < pronum; j++) {
//                // 计算 derivative
//                Element I = derivative(pairingParametersFileName, chall, chall[j]);
//                // 计算 c = c / I
//                c.set(c).div(I);
//                // 计算 wit_c[j] = witness[j]^c
//                wit_c[j] = witness[j].duplicate().powZn(c);
//            }

            for(int j=0;j<pronum;j++){
                Element c = bp.getZr().newOneElement().getImmutable();
                Element I = derivative(pairingParametersFileName,chall,chall[j]);
                c=c.div(I);
                wit_c[j]=witness[j].duplicate().powZn(c);
//                System.out.println(I);
//                System.out.println(c);
//                System.out.println(wit_c[j]);
            }

            Element temp;
            temp= chall[i].duplicate().add(s);
            challI=challI.duplicate().mul(temp);
            //System.out.println(wit_c[i]);
            witI=witI.duplicate().mul(wit_c[i]);
        }
        //优化版本
//        for (int i = 0; i < pronum; i++) {
//            // 缓存当前 i 所需的所有导数结果，避免重复计算
//            long startTime = System.currentTimeMillis();
//            Element[] cachedDerivatives = new Element[pronum];
//            for (int j = 0; j < pronum; j++) {
//                cachedDerivatives[j] = derivative(pairingParametersFileName, chall, chall[j]);
//            }
//            long endTime = System.currentTimeMillis();
//            System.out.println(endTime-startTime);
//            // 初始化 wit_c 的累积值
//            Element temp = chall[i].duplicate().add(s); // 计算 (chall[i] + s)
//            challI.mul(temp); // 累乘到 challI 中
//            for (int j = 0; j < pronum; j++) {
//                // 使用缓存的导数结果计算 c
//                Element c = bp.getZr().newOneElement();
//                c.div(cachedDerivatives[j]);
//                // wit_c[j] 的值更新
//                wit_c[j] = witness[j].duplicate().powZn(c);
//                // 累乘 witI
//                if (i == 0) {
//                    witI.set(wit_c[j]);
//                } else {
//                    witI.mul(wit_c[j]);
//                }
//            }
//        }

        //把witnessI写入到文件里
        Properties witnessI_prop = new Properties();
        // 将g转换为字符串后写入，但文件中显示乱码，为避免乱码采用Base64编码方式
        witnessI_prop.setProperty("I_chall", Base64.getEncoder().encodeToString(challI.toBytes()));
        witnessI_prop.setProperty("witnessI", Base64.getEncoder().encodeToString(witI.toBytes()));
        storePropFile(witnessI_prop,witFileName+ "witnessI.properties");

    }

    public static void Verify_agg_witness(String pairingParametersFileName,String sFileName,String gFileName,String accFileName,String witFileName){
        Pairing bp = PairingFactory.getPairing(pairingParametersFileName);
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

        //读取accB
        Properties acc_prop =new Properties();
        try{
            acc_prop.load(new FileInputStream(accFileName));
        }catch(IOException ex){
            ex.printStackTrace();
        }
        String accString = acc_prop.getProperty("accB");
        Element accB = bp.getG1().newElementFromBytes(Base64.getDecoder().decode(accString)).getImmutable();

        //读取witnessI
        Properties witnessI_prop =new Properties();
        try{
            witnessI_prop.load(new FileInputStream(witFileName));
        }catch(IOException ex){
            ex.printStackTrace();
        }
        String IchalString = witnessI_prop.getProperty("I_chall");
        Element I_chall = bp.getZr().newElementFromBytes(Base64.getDecoder().decode(IchalString)).getImmutable();
        String witIString = witnessI_prop.getProperty("witnessI");
        Element witnessI = bp.getG1().newElementFromBytes(Base64.getDecoder().decode(witIString)).getImmutable();
        //双线性映射计算
        Element egg_acc = bp.pairing(accB,g);
        Element g_I_chall = g.duplicate().powZn(I_chall);

        Element egg_witI = bp.pairing(g_I_chall,witnessI);
        if (egg_acc.isEqual(egg_witI)) {
            System.out.println("yes");
        }else
            System.out.println("no");
    }

    public static void Verify_witness(String pairingParametersFileName,String sFileName,String gFileName,String accFileName,String witFileName){
        Pairing bp = PairingFactory.getPairing(pairingParametersFileName);
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

        //读取accB
        Properties acc_prop =new Properties();
        try{
            acc_prop.load(new FileInputStream(accFileName));
        }catch(IOException ex){
            ex.printStackTrace();
        }
        String accString = acc_prop.getProperty("accB");
        Element accB = bp.getG1().newElementFromBytes(Base64.getDecoder().decode(accString)).getImmutable();

        //读取witness
        Properties witness_prop =new Properties();
        try{
            witness_prop.load(new FileInputStream(witFileName));
        }catch(IOException ex){
            ex.printStackTrace();
        }
        String bchalString = witness_prop.getProperty("b_chall");
        Element b_chall = bp.getZr().newElementFromBytes(Base64.getDecoder().decode(bchalString)).getImmutable();
        String witString = witness_prop.getProperty("witness");
        Element witness = bp.getG1().newElementFromBytes(Base64.getDecoder().decode(witString)).getImmutable();
        //双线性映射计算
        Element egg_acc = bp.pairing(accB,g);
        Element bchall_s= b_chall.duplicate().add(s);
        Element g_bchal_s = g.duplicate().powZn(bchall_s);

        Element egg_wit = bp.pairing(g_bchal_s,witness);
        if (egg_acc.isEqual(egg_wit)) {
            System.out.println("yes");
        }else
            System.out.println("no");
    }

    public static void main(String[] args) throws Exception{
        String Filepath = "D:\\Code\\IDEA\\Decentralized_storage_auditor\\Data\\date\\10mb.doc";
        String dir = "data/";
        String pairingParametersFileName = "a.properties";
        String gFileName = dir + "g.properties";
        String sFileName = dir + "s.properties";
        String accFileName = dir + "acc.properties";
        int blocksize =4*1024;

//        long startTime = System.currentTimeMillis();  //测试算法执行时间的开始时间
//        //密钥生成
//        setup(pairingParametersFileName,gFileName,sFileName);
//        long endTime = System.currentTimeMillis();
//        System.out.println(endTime-startTime);

//        long startTime = System.currentTimeMillis();  //测试算法执行时间的开始时间
//          //生成累积值
//        accGen(Filepath,pairingParametersFileName,blocksize,sFileName,gFileName,accFileName);
//        long endTime = System.currentTimeMillis();
//        System.out.println(endTime-startTime);

          //分割数据块
//        long startTime = System.currentTimeMillis();  //测试算法执行时间的开始时间
//        String chunkPath = dir + "datachunk_10mb/";
//        Split_Data(Filepath,blocksize,chunkPath);
//        long endTime = System.currentTimeMillis();
//        System.out.println(endTime-startTime);

          //合并数据块
//        String chunkpath = "data/datachunk/";
//        String mergepath = "data/123.txt";
//        Merge_Data(chunkpath,mergepath);

        //D为1时，测试证明的时间消耗
//        long startTime = System.currentTimeMillis();  //测试算法执行时间的开始时间
//        String witFileName = "StorageProvider/" + "witness.properties";
//        Verify_witness(pairingParametersFileName,sFileName,gFileName,accFileName,witFileName);
//        long endTime = System.currentTimeMillis();
//        System.out.println(endTime-startTime);

        //生成证明辅助参数
//        long startTime = System.currentTimeMillis();  //测试算法执行时间的开始时间
//        int provider_num = 5;
//        String witparamfileName = dir +"witparam.properties";
//        witparamGen(pairingParametersFileName,Filepath,provider_num,blocksize,sFileName,gFileName,witparamfileName);
//        long endTime = System.currentTimeMillis();
//        System.out.println(endTime-startTime);

        //证明生成辅助参数
//        long startTime = System.currentTimeMillis();  //测试算法执行时间的开始时间
//        int providernum =20;
//        String chunkPath = dir + "datachunk_10mb/";
//        String paramfileName = "StorageProvider/" +"param.properties";
//        paramGen(pairingParametersFileName,Filepath,providernum,blocksize,sFileName,chunkPath,paramfileName);
//        long endTime = System.currentTimeMillis();
//        System.out.println(endTime-startTime);

        //验证部分块的证明
//        long startTime = System.currentTimeMillis();  //测试算法执行时间的开始时间
//        String witFileName = "StorageProvider/" + "witness1.properties";
//        Verify_witness(pairingParametersFileName,sFileName,gFileName,accFileName,witFileName);
//        long endTime = System.currentTimeMillis();
//        System.out.println(endTime-startTime);

        //测试聚合证明
//        long startTime = System.currentTimeMillis();  //测试算法执行时间的开始时间
//        int pronum = 20;
//        String witFileName = "StorageProvider/";
//        aggregation_witness(pronum,pairingParametersFileName,sFileName,gFileName,witFileName);
//        long endTime = System.currentTimeMillis();
//        System.out.println(endTime-startTime);

        //验证聚合证明
//        long startTime = System.currentTimeMillis();  //测试算法执行时间的开始时间
//        String witFileName = "StorageProvider/"+"witnessI.properties";
//        Verify_agg_witness(pairingParametersFileName,sFileName,gFileName,accFileName,witFileName);
//        long endTime = System.currentTimeMillis();
//        System.out.println(endTime-startTime);

          //D为一个时候，验证算法的时间开销
//          String witFileName = "StorageProvider/" + "witness.properties";
//          long startTime = System.currentTimeMillis();  //测试算法执行时间的开始时间
//          Verify_witness(pairingParametersFileName,sFileName,gFileName,accFileName,witFileName);
//          long endTime = System.currentTimeMillis();
//          System.out.println(endTime-startTime);

//        long startTime = System.currentTimeMillis();  //测试算法执行时间的开始时间
//        long endTime = System.currentTimeMillis();
//        System.out.println(endTime-startTime);


          //测试对部分证明的验证
//        //读取accb
//        Pairing bp = PairingFactory.getPairing(pairingParametersFileName);
//        Properties acc_prop =new Properties();
//        try {
//            acc_prop.load(new FileInputStream(accFileName));
//        }catch(IOException ex){
//            ex.printStackTrace();
//        }
//        String accString = acc_prop.getProperty("accB");
//        Element accB = bp.getG1().newElementFromBytes(Base64.getDecoder().decode(accString)).getImmutable();
//
//        //读取g
//        Properties g_prop =new Properties();
//        try{
//            g_prop.load(new FileInputStream(gFileName));
//        }catch(IOException ex){
//            ex.printStackTrace();
//        }
//        String gString = g_prop.getProperty("g");
//        Element g = bp.getG1().newElementFromBytes(Base64.getDecoder().decode(gString)).getImmutable();
//
//        //读取param1
//        Properties s_prop =new Properties();
//        try {
//            s_prop.load(new FileInputStream(paramfileName));
//        }catch(IOException ex){
//            ex.printStackTrace();
//        }
//        String sString = s_prop.getProperty("param0");
//        Element param0 = bp.getZr().newElementFromBytes(Base64.getDecoder().decode(sString)).getImmutable();
//        //读取param2
//        Properties s_prop2 =new Properties();
//        try {
//            s_prop2.load(new FileInputStream(paramfileName));
//        }catch(IOException ex){
//            ex.printStackTrace();
//        }
//        String sString2 = s_prop2.getProperty("param1");
//        Element param1 = bp.getZr().newElementFromBytes(Base64.getDecoder().decode(sString2)).getImmutable();
//        Element param0_param1 = param1.duplicate().mul(param0);
//        Element acc = g.duplicate().powZn(param0_param1);
//        if(acc.isEqual(accB)){
//            System.out.println("yes");
//        }



    }

}
