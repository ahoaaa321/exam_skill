package draft;

import java.util.Comparator;
import java.util.List;

public class StudApp {

    public record Student(
            int studentId,
            String name,
            String gender,
            int chinese,
            int math,
            int foreignLanguage
    ) {
        public int totalScore() {
            return chinese + math + foreignLanguage;
        }
    }

    public static void main(String[] args) {
        List<Student> students = List.of(
                new Student(100, "zhang3", "M", 78, 56, 92),
                new Student(103, "li4", "F", 92, 77, 84),
                new Student(104, "wang5", "M", 98, 44, 82),
                new Student(106, "liu6", "F", 87, 73, 98)
        );

        students.stream()
                .filter(student -> "F".equals(student.gender()))
                .sorted(Comparator.comparingInt(Student::totalScore).reversed())
                .forEach(student -> System.out.printf(
                        "name=%s, chinese=%d, math=%d, foreignLanguage=%d, total=%d%n",
                        student.name(),
                        student.chinese(),
                        student.math(),
                        student.foreignLanguage(),
                        student.totalScore()
                ));
    }
}