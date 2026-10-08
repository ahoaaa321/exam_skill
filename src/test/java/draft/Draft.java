package draft;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class Draft {

    /**
     * 订单类：练习 5 电商订单数据分析使用。
     */
    public record Order(String orderId, String product, double amount, String category) {
    }

    public static void main(String[] args) {
        exercise1();
        System.out.println("----------------------------------------");
        exercise2();
        System.out.println("----------------------------------------");
        exercise3();
        System.out.println("----------------------------------------");
        exercise4();
        System.out.println("----------------------------------------");
        exercise5();
    }

    /**
     * 练习 1：排序综合练习
     * 给定字符串列表 ["Java", "Python", "C", "JavaScript", "Go", "Ruby"]
     * (1) 按字典序升序排列
     * (2) 按字典序降序排列
     */
    private static void exercise1() {
        System.out.println("=== 练习 1：排序综合练习 ===");
        List<String> words = List.of("Java", "Python", "C", "JavaScript", "Go", "Ruby");
        System.out.println("原始列表: " + words);

        // (1) 按字典序升序排列（自然顺序）
        List<String> ascending = words.stream()
                .sorted()
                .collect(Collectors.toList());
        System.out.println("(1) 字典序升序: " + ascending);

        // (2) 按字典序降序排列（使用 Comparator.reverseOrder()）
        List<String> descending = words.stream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        System.out.println("(2) 字典序降序: " + descending);
    }

    /**
     * 练习 2：分页与截取练习
     * 给定一个包含 20 个整数的列表（1~20）
     * (1) 使用 skip + limit 获取第 3 页数据（每页 5 条）
     * (2) 先降序排序，再取前 5 个最大值
     * (3) 先升序排序，跳过前 5 个，取接下来的 5 个（即第 6~10 小的值）
     * (4) 思考：如果 skip 的数量超过列表大小，结果是什么？
     */
    private static void exercise2() {
        System.out.println("=== 练习 2：分页与截取练习 ===");
        List<Integer> nums = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                11, 12, 13, 14, 15, 16, 17, 18, 19, 20);
        System.out.println("原始列表: " + nums);

        int pageSize = 5;

        // (1) 获取第 3 页数据（每页 5 条）
        // 通用公式: skip((page - 1) * pageSize).limit(pageSize)
        int page = 3;
        List<Integer> page3 = nums.stream()
                .skip((long) (page - 1) * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
        System.out.println("(1) 第 3 页数据: " + page3);

        // (2) 先降序排序，再取前 5 个最大值
        List<Integer> top5 = nums.stream()
                .sorted(Comparator.reverseOrder())
                .limit(5)
                .collect(Collectors.toList());
        System.out.println("(2) 前 5 个最大值: " + top5);

        // (3) 先升序排序，跳过前 5 个，取接下来的 5 个（第 6~10 小的值）
        List<Integer> mid5 = nums.stream()
                .sorted()
                .skip(5)
                .limit(5)
                .collect(Collectors.toList());
        System.out.println("(3) 第 6~10 小的值: " + mid5);

        // (4) 思考：skip 的数量超过列表大小，返回空流
        List<Integer> overSkip = nums.stream()
                .skip(50)
                .collect(Collectors.toList());
        System.out.println("(4) skip(50) 超过列表大小的结果: " + overSkip
                + "  => skip(n) 当 n 超过流大小时返回空流");
    }

    /**
     * 练习 3：去重练习
     * 给定整数列表 [7, 3, 5, 3, 7, 1, 5, 9, 1, 9]
     * (1) 使用 distinct() 去重，输出不重复的元素
     * (2) 去重后降序排列
     * (3) 去重后升序排列，取中间值（即跳过一半，取一个）
     * (4) 使用 distinct() + count() 统计不重复元素的个数
     */
    private static void exercise3() {
        System.out.println("=== 练习 3：去重练习 ===");
        List<Integer> nums = List.of(7, 3, 5, 3, 7, 1, 5, 9, 1, 9);
        System.out.println("原始列表: " + nums);

        // (1) 使用 distinct() 去重，输出不重复的元素（保留首次出现顺序）
        List<Integer> unique = nums.stream()
                .distinct()
                .collect(Collectors.toList());
        System.out.println("(1) 去重后: " + unique);

        // (2) 去重后降序排列
        List<Integer> descUnique = nums.stream()
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        System.out.println("(2) 去重后降序: " + descUnique);

        // (3) 去重后升序排列，取中间值（跳过一半，取一个）
        List<Integer> ascUnique = nums.stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        System.out.println("(3) 去重后升序: " + ascUnique);
        Integer middle = ascUnique.stream()
                .skip(ascUnique.size() / 2)
                .findFirst()
                .orElse(null);
        System.out.println("    中间值: " + middle);

        // (4) 使用 distinct() + count() 统计不重复元素的个数
        long count = nums.stream()
                .distinct()
                .count();
        System.out.println("(4) 不重复元素个数: " + count);
    }

    /**
     * 练习 4：peek 调试练习
     * 给定字符串列表 ["Hello", "World", "Java", "Stream", "API"]
     * (1) 筛选长度大于 3 的字符串
     * (2) 转为大写
     * (3) 排序
     * (4) 收集到列表
     * 在每个操作前后使用 peek 打印当前数据，观察数据的变化过程。
     */
    private static void exercise4() {
        System.out.println("=== 练习 4：peek 调试练习 ===");
        List<String> words = List.of("Hello", "World", "Java", "Stream", "API");
        System.out.println("原始列表: " + words);

        List<String> result = words.stream()
                .peek(w -> System.out.println("  [原始] " + w))
                // (1) 筛选长度大于 3 的字符串
                .filter(w -> w.length() > 3)
                .peek(w -> System.out.println("  [筛选后 length>3] " + w))
                // (2) 转为大写
                .map(String::toUpperCase)
                .peek(w -> System.out.println("  [转大写后] " + w))
                // (3) 排序
                .sorted()
                .peek(w -> System.out.println("  [排序后] " + w))
                // (4) 收集到列表
                .collect(Collectors.toList());

        System.out.println("(4) 最终结果: " + result);
    }

    
    private static void exercise5() {
        System.out.println("=== 练习 5：电商订单数据分析 ===");
        List<Order> orders = List.of(
                new Order("O001", "Java 核心技术", 128.0, "图书"),
                new Order("O002", "无线鼠标", 89.0, "电子"),
                new Order("O003", "机械键盘", 359.0, "电子"),
                new Order("O004", "纯棉 T 恤", 79.0, "服装"),
                new Order("O005", "蓝牙耳机", 199.0, "电子"),
                new Order("O006", "牛仔裤", 259.0, "服装"),
                new Order("O007", "算法导论", 156.0, "图书"),
                new Order("O008", "运动跑鞋", 499.0, "服装"),
                new Order("O009", "坚果礼盒", 138.0, "食品"),
                new Order("O010", "USB-C 拓展坞", 189.0, "电子"),
                new Order("O011", "咖啡豆", 68.0, "食品"),
                new Order("O012", "羽绒服", 899.0, "服装")
        );
        System.out.println("订单总数: " + orders.size());

        // (1) 按金额降序排列，取出金额最高的 3 笔订单
        // (3) 使用 peek 在第(1)步中观察排序后的数据
        List<Order> top3 = orders.stream()
                .sorted(Comparator.comparingDouble(Order::amount).reversed())
                .peek(o -> System.out.println("  peek 排序后: " + o))
                .limit(3)
                .collect(Collectors.toList());
        System.out.println("(1) 金额最高的 3 笔订单:");
        top3.forEach(o -> System.out.println("    " + o));

        // (2) 筛选金额大于 100 的订单，按金额升序排列，跳过前 2 笔，取接下来的 3 笔
        List<Order> filtered = orders.stream()
                .filter(o -> o.amount() > 100)
                .sorted(Comparator.comparingDouble(Order::amount))
                .skip(2)
                .limit(3)
                .collect(Collectors.toList());
        System.out.println("(2) 金额>100 升序，跳过前 2 取接下来 3 笔:");
        filtered.forEach(o -> System.out.println("    " + o));

        // (4) 按分类分组，每组取金额最高的订单（综合 groupingBy + sorted + limit）
        Map<String, List<Order>> topByCategory = orders.stream()
                .collect(Collectors.groupingBy(
                        Order::category,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .sorted(Comparator.comparingDouble(Order::amount).reversed())
                                        .limit(1)
                                        .collect(Collectors.toList())
                        )
                ));
        System.out.println("(4) 每个分类金额最高的订单:");
        topByCategory.forEach((cat, list) ->
                System.out.println("    " + cat + " -> " + list.get(0)));
    }
}
