package lab.stream;

/** 学生成绩对象。 */
public class Student {
    public final String name;
    public final int chinese;
    public final int math;
    public final int english;

    public Student(String name, int chinese, int math, int english) {
        this.name = name;
        this.chinese = chinese;
        this.math = math;
        this.english = english;
    }
    public int total() {
        return chinese + math + english;
    }

    @Override
    public String toString() {
        return name + "(语文=" + chinese + ",数学=" + math
                + ",英语=" + english + ",总分=" + total() + ")";
    }
}