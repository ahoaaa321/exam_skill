package draft;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class draft1 {


    public record Order(String orderId, String product, double amount, String category) {
    }

    public static void main(String[] args) {
        exercise5();
    }


    private static void exercise5() {
        System.out.println("练习 5");
        List<Order> orders = List.of(
                new Order("O001", "深入理解计算机系统", 218.60, "图书"),
                new Order("O002", "静音键盘", 156.30, "数码"),
                new Order("O003", "4K 显示器", 1299.00, "数码"),
                new Order("O004", "羊毛大衣", 759.00, "服饰"),
                new Order("O005", "降噪耳机", 599.00, "数码"),
                new Order("O006", "休闲西裤", 329.00, "服饰"),
                new Order("O007", "代码整洁之道", 98.50, "图书"),
                new Order("O008", "真皮皮鞋", 459.00, "服饰"),
                new Order("O009", "进口牛排套装", 268.00, "食品"),
                new Order("O010", "智能手表", 879.00, "数码"),
                new Order("O011", "云南小粒咖啡", 76.80, "食品"),
                new Order("O012", "冲锋衣", 639.00, "服饰"),
                new Order("O013", "精华液套装", 388.00, "美妆"),
                new Order("O014", "跑步机", 2199.00, "运动"),
                new Order("O015", "扫地机器人", 1599.00, "家居"),
                new Order("O016", "香水礼盒", 498.00, "美妆"),
                new Order("O017", "登山背包", 259.00, "运动"),
                new Order("O018", "乳胶枕", 189.00, "家居")
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
