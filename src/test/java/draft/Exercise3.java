package draft;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 练习 3：分组操作练习。
 */
public class Exercise3 {

    /** 学生信息：姓名和班级。 */
    public record Student(String name, String grade) {
        public Student {
            Objects.requireNonNull(name, "name cannot be null");
            Objects.requireNonNull(grade, "grade cannot be null");
        }
    }

    /**
     * 按班级分组，得到 Map<String, List<Student>>。
     */
    public static Map<String, List<Student>> groupByGrade(List<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(Student::grade));
    }

    /**
     * 按班级统计每个班级的学生人数，得到 Map<String, Long>。
     */
    public static Map<String, Long> countByGrade(List<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(Student::grade, Collectors.counting()));
    }

    /**
     * 按班级分组，并提取每个班级的学生姓名，得到 Map<String, List<String>>。
     */
    public static Map<String, List<String>> namesByGrade(List<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(
                        Student::grade,
                        Collectors.mapping(Student::name, Collectors.toList())
                ));
    }

    public static void main(String[] args) {
        List<Student> students = List.of(
                new Student("张三", "一班"),
                new Student("李四", "二班"),
                new Student("王五", "一班"),
                new Student("赵六", "三班"),
                new Student("钱七", "二班")
        );

        System.out.println("按班级分组：" + groupByGrade(students));
        System.out.println("各班人数：" + countByGrade(students));
        System.out.println("各班姓名：" + namesByGrade(students));
    }
}