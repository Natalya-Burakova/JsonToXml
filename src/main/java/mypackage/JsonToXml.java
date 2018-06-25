package mypackage;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Stack;
import java.util.StringTokenizer;

class JsonToXml {

    static String state = ""; //состояние конечного автомата
    static Stack finalTag = new Stack(); //стэк для хранения тэгов
    static Object mem; //хранит тэг который снимается со стэка
    //переменные для проверки баланса скобок
    static int bracketsStart = 0;
    static int bracketsEnd = 0;
    static int square_bracketsStart = 0;
    static int square_bracketsEnd = 0;

    public static void main(String[] args) throws IOException {
        if(args.length != 3)
            System.exit(-1);
        String json = args[0]; //json
        String xmlOut = args[1]; //xml
        String flag = args[2]; //флаг форматирования
        //String json = "src/input.json";
        //String xmlOut = "src/out.xml";
        //String flag = "true";
        writeFile(readFile(json, flag), xmlOut);
    }

    //читаем json и формируем xml
    public static String readFile(String json, String flag){
        Console c = System.console();
        StringBuilder store = new StringBuilder();
        try (FileInputStream f = new FileInputStream(json);
             BufferedReader br = new BufferedReader(new InputStreamReader(f));) {
            String strLine;
            while ((strLine = br.readLine()) != null) {
                String xml = parse(strLine); //вызываем функцию для парсинга  json
                if (xml.equals("Error")) {
                    c.printf("Неверный синтаксис json");
                    store = new StringBuilder();
                    break;
                }
                if (flag.equals("true"))
                    store.append(xml + "\n");
                else if (flag.equals("false"))
                    store.append(xml);
                else {
                    c.printf("Неверный флаг форматирования");
                    store = new StringBuilder();
                    break;
                }
            }
        } catch (IOException e) {
            c.printf("Ошибка в чтении файла: %s \n", json.toString());
        }
        if (square_bracketsStart!=square_bracketsEnd || bracketsStart!=bracketsEnd){
            store = new StringBuilder();
            c.printf("Нарушен формат скобок");
        }
        return store.toString();
    }

    //записываем результат в файл
    public static void writeFile(String strLine, String xmlOut) {
        Console c = System.console();
        try (FileWriter ofstream = new FileWriter(xmlOut);
             BufferedWriter out = new BufferedWriter(ofstream);) {
            out.write(strLine);
            out.flush();
            c.printf("Успешно!");
        }
        catch (IOException e) {
            c.printf("Ошибка записи в файл: %s \n", xmlOut.toString());
        }
    }

    //конечный автомат для парсинга Json
    public static String parse(String str){
        //для формирования новой строки в формате xml
        StringBuilder newStr = new StringBuilder();
        //выделяем токены ...
        //токенами являются: { } [ ] , : " любое слово
        StringTokenizer tokenizer = new StringTokenizer(str, "{\":,[]}", true);
        while (tokenizer.hasMoreTokens()){
            //текущий токен
            String s = tokenizer.nextToken();
            if (s.equals("{")){
                if (state.equals("mid") || state.equals("mid-x"))
                    state = "start-x";
                else
                    state = "start";
                bracketsStart++;
            }
            else if (s.equals("\"")) {
                if(state.equals("start")) {
                    state = "start-1";
                    newStr.append("<");
                }
                else if (state.equals("start-2")){
                    state = "end";
                    newStr.append(">");
                }
                else if (state.equals("mid"))
                    state = "mid-2";
                else if(state.equals("mid-2")) {
                    state ="mid-end";
                    newStr.append("</" + finalTag.pop() + ">");
                }
                else if (state.equals("start-x")){
                    state = "start-x1";
                    newStr.append("<");
                }
                else if (state.equals("start-x2")){
                    newStr.append(">");
                    state = "start-x3";
                }
                else if (state.equals("mid-x"))
                    state = "mid-x2";
                else if (state.equals("mid-x2")){
                    state = "mid-x-end";
                    newStr.append("</" + finalTag.pop() + ">");
                }
                else if (state.equals("new")) {
                    mem = finalTag.pop();
                    int k =mem.toString().lastIndexOf("s");
                    newStr.append("<" + mem.toString().substring(0, k)+ ">");
                    finalTag.push(mem);
                    state = "new-2";
                }
                else if (state.equals("new-3")) {
                    state = "new-end";
                    int k =mem.toString().lastIndexOf("s");
                    mem = finalTag.pop();
                    finalTag.push(mem);
                    newStr.append("</" + mem.toString().substring(0,k) + ">");
                }
                else
                    state = "error";
            }
            else if (s.equals(",")) {
                if (state.equals("mid-end"))
                    state = "start";
                else if (state.equals("mid-x-end") || state.equals("finish") ||  state.equals("new-finish"))
                    state = "start-x";
                else if (state.equals("new-end"))
                    state = "new";
                else
                    state = "error";
            }
            else if (s.equals("[")) {
                if (state.equals("mid") || state.equals("mid-x"))
                    state = "new";
                else
                    state = "error";
                square_bracketsStart++;
            }
            else if (s.equals("]")) {
                if (state.equals("new-end")) {
                    state = "new-finish";
                    newStr.append("</" + finalTag.pop()+ ">");
                }
                else
                    state = "error";
                square_bracketsEnd++;
            }
            else if (s.equals(":")){
                if (state.equals("end"))
                    state = "mid";
                else if (state.equals("start-x3"))
                    state = "mid-x";
                else
                    state = "error";
            }
            else if (s.equals("}")) {
                if (state.equals("mid-x-end")) {
                    state = "finish";
                    newStr.append("</" + finalTag.pop() + ">");
                }
                else if (state.equals("finish") && !finalTag.empty()){
                    state = "finish";
                    newStr.append("</" + finalTag.pop() + ">");
                }
                else if (state.equals("finish")) {
                    state = "Myfinish";
                }
                else if (state.equals("new-finish") && !finalTag.empty()){
                    state = "finish";
                    newStr.append("</" + finalTag.pop() + ">");
                }
                else
                    state = "error";
                bracketsEnd++;
            }
            else {
                if (state.equals("start-1")) {
                    finalTag.push(s);
                    state = "start-2";
                }
                else if (state.equals("start-x1")) {
                    finalTag.push(s);
                    state = "start-x2";
                }
                else if (state.equals("new-2"))
                    state = "new-3";
                newStr.append(s);
            }
        }
        if (state.equals("error"))
            return "Error";
        return newStr.toString();

    }

}