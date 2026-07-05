package com.example.cookup_app.utils;

import com.example.cookup_app.R;
import com.example.cookup_app.model.Ingredient;
import com.example.cookup_app.model.Recipe;
import com.example.cookup_app.model.RecipeStep;

import java.util.ArrayList;
import java.util.List;

public class RecipeDataHelper {

    public static Recipe populateDetails(Recipe r) {
        if (r == null) return null;

        // Ensure description, country, difficulty are set
        if (r.getCountry() == null || r.getCountry().isEmpty()) {
            r.setCountry("Việt Nam");
        }
        if (r.getDifficulty() == null || r.getDifficulty().isEmpty()) {
            r.setDifficulty("Dễ");
        }
        if (r.getServings() <= 0) {
            r.setServings(4); // Default servings
        }

        String name = r.getName().toLowerCase();

        if (name.contains("phở bò") || name.contains("pho bo") || name.contains("phở")) {
            r.setServings(4);
            r.setDifficulty("Trung bình");
            r.setDescription("Món phở bò gia truyền đậm đà hương vị Hà Nội xưa. Nước dùng thanh ngọt hầm từ xương ống bò trong nhiều giờ kết hợp với quế, hồi, thảo quả nướng thơm lừng.");
            
            List<Ingredient> ingredients = new ArrayList<>();
            ingredients.add(new Ingredient("500g", "Thịt bò thăn"));
            ingredients.add(new Ingredient("1kg", "Xương ống bò"));
            ingredients.add(new Ingredient("1 củ", "Hành tây"));
            ingredients.add(new Ingredient("1 củ", "Hành tím"));
            ingredients.add(new Ingredient("1 nhánh", "Gừng tươi"));
            ingredients.add(new Ingredient("3 cái", "Hoa hồi"));
            ingredients.add(new Ingredient("1 miếng", "Vỏ quế"));
            ingredients.add(new Ingredient("1 quả", "Thảo quả"));
            ingredients.add(new Ingredient("Vừa đủ", "Bánh phở, hành lá, ngò rí"));
            r.setIngredients(ingredients);

            List<RecipeStep> steps = new ArrayList<>();
            
            RecipeStep step1 = new RecipeStep(1, "Nướng chín hành tây, hành tím và gừng tươi trên bếp đến khi cháy xém thơm lừng để tạo mùi thơm sâu cho nước dùng phở bò.", "", 300);
            step1.getStepIngredients().add(new Ingredient("1 củ", "Hành tây"));
            step1.getStepIngredients().add(new Ingredient("1 củ", "Hành tím"));
            step1.getStepIngredients().add(new Ingredient("1 nhánh", "Gừng tươi"));
            steps.add(step1);

            RecipeStep step2 = new RecipeStep(2, "Trần sơ xương ống bò qua nước sôi có pha chút muối hạt và gừng đập dập trong 5 phút để loại bỏ hoàn toàn cặn bẩn và bọt đen.", "", 300);
            step2.getStepIngredients().add(new Ingredient("1kg", "Xương ống bò"));
            steps.add(step2);

            RecipeStep step3 = new RecipeStep(3, "Hầm xương ống bò nhỏ lửa trong ít nhất 4-6 tiếng. Thêm hành gừng nướng, quế, hồi, thảo quả đã rang thơm vào nồi hầm trong 1 giờ cuối.", "", 1500); // 25 mins simulated timer
            step3.getStepIngredients().add(new Ingredient("1kg", "Xương ống bò"));
            step3.getStepIngredients().add(new Ingredient("3 cái", "Hoa hồi"));
            step3.getStepIngredients().add(new Ingredient("1 miếng", "Vỏ quế"));
            step3.getStepIngredients().add(new Ingredient("1 quả", "Thảo quả"));
            steps.add(step3);

            RecipeStep step4 = new RecipeStep(4, "Xếp bánh phở trần nóng vào tô lớn, bày thịt bò thăn thái lát thật mỏng lên trên mặt bún cùng với hành lá, ngò rí cắt nhỏ.", "", 90);
            step4.getStepIngredients().add(new Ingredient("Vừa đủ", "Bánh phở, hành lá, ngò rí"));
            step4.getStepIngredients().add(new Ingredient("500g", "Thịt bò thăn"));
            steps.add(step4);

            RecipeStep step5 = new RecipeStep(5, "Chan nước dùng phở hầm đang sôi sùng sục trực tiếp lên lát thịt bò để chín tái hoàn hảo, thưởng thức kèm chanh ớt và quẩy giòn ngon.", "", 60);
            step5.getStepIngredients().add(new Ingredient("500g", "Thịt bò thăn"));
            steps.add(step5);

            r.setSteps(steps);

        } else if (name.contains("bún bò") || name.contains("bun bo") || name.contains("bún")) {
            r.setServings(4);
            r.setDifficulty("Khó");
            r.setDescription("Đậm đà hương vị cố đô Huế với nước dùng đậm vị sả nồng nàn và mắm ruốc chưng dậy mùi quyến rũ, ăn cùng giò heo dẻo dai và bắp bò mềm ngậy ngon mắt.");
            
            List<Ingredient> ingredients = new ArrayList<>();
            ingredients.add(new Ingredient("500g", "Bắp bò"));
            ingredients.add(new Ingredient("800g", "Giò heo"));
            ingredients.add(new Ingredient("300g", "Chả cua hoặc mọc"));
            ingredients.add(new Ingredient("6 cây", "Sả tươi"));
            ingredients.add(new Ingredient("3 muỗng", "Mắm ruốc Huế"));
            ingredients.add(new Ingredient("200g", "Huyết heo (tùy chọn)"));
            ingredients.add(new Ingredient("1 củ", "Hành tây"));
            ingredients.add(new Ingredient("Vừa đủ", "Rau thơm, hoa chuối, giá đỗ"));
            r.setIngredients(ingredients);

            List<RecipeStep> steps = new ArrayList<>();

            RecipeStep step1 = new RecipeStep(1, "Sả cây lột vỏ xơ, đập dập rồi xếp gọn gàng xuống đáy nồi áp suất hoặc nồi hầm lớn. Đặt giò heo dẻo và thịt bắp bò nguyên miếng lên trên.", "", 180);
            step1.getStepIngredients().add(new Ingredient("6 cây", "Sả tươi"));
            step1.getStepIngredients().add(new Ingredient("800g", "Giò heo"));
            step1.getStepIngredients().add(new Ingredient("500g", "Bắp bò"));
            steps.add(step1);

            RecipeStep step2 = new RecipeStep(2, "Hòa tan mắm ruốc Huế đặc trưng với 1 tô nước lạnh sạch, khuấy đều rồi để lắng cặn. Gạn lấy phần nước trong đổ vào nồi xương hầm thơm lừng.", "", 240);
            step2.getStepIngredients().add(new Ingredient("3 muỗng", "Mắm ruốc Huế"));
            steps.add(step2);

            RecipeStep step3 = new RecipeStep(3, "Phi thơm hành tím băm, tỏi băm, sả băm nát và chút ớt bột sấy khô trong dầu điều nóng hổi để tạo nước màu đỏ sẫm đặc sắc của tô Bún Bò Huế.", "", 150);
            step3.getStepIngredients().add(new Ingredient("6 cây", "Sả tươi"));
            steps.add(step3);

            RecipeStep step4 = new RecipeStep(4, "Trút toàn bộ dầu màu sả ớt đã phi thơm vàng thơm óng ánh vào nồi nước dùng bún bò, cho huyết heo và mọc chả cua chín mềm vào đun nóng hổi.", "", 300);
            step4.getStepIngredients().add(new Ingredient("200g", "Huyết heo (tùy chọn)"));
            step4.getStepIngredients().add(new Ingredient("300g", "Chả cua hoặc mọc"));
            steps.add(step4);

            RecipeStep step5 = new RecipeStep(5, "Xếp sợi bún to Huế vào tô, đặt lát bắp bò thái mỏng dính, giò heo dẻo dai, chả cua, chan ngập nước dùng sôi đỏ cam rồi ăn kèm rổ rau sống tươi ngon.", "", 90);
            step5.getStepIngredients().add(new Ingredient("Vừa đủ", "Rau thơm, hoa chuối, giá đỗ"));
            steps.add(step5);

            r.setSteps(steps);

        } else if (name.contains("bánh mì") || name.contains("banh mi")) {
            r.setServings(2);
            r.setDifficulty("Dễ");
            r.setDescription("Ổ bánh mì giòn tan trứ danh Việt Nam kẹp nhân thịt vai heo nướng mật ong sả nồng nàn thơm nức mũi kết hợp đồ chua cay ngọt và ngò rí thơm mát.");
            
            List<Ingredient> ingredients = new ArrayList<>();
            ingredients.add(new Ingredient("400g", "Thịt nạc vai heo"));
            ingredients.add(new Ingredient("2 muỗng", "Sả băm"));
            ingredients.add(new Ingredient("1 muỗng", "Mật ong nguyên chất"));
            ingredients.add(new Ingredient("2 ổ", "Bánh mì giòn nóng"));
            ingredients.add(new Ingredient("Vừa đủ", "Dưa leo, đồ chua, ngò rí"));
            ingredients.add(new Ingredient("1 muỗng", "Dầu hào, nước tương"));
            r.setIngredients(ingredients);

            List<RecipeStep> steps = new ArrayList<>();

            RecipeStep step1 = new RecipeStep(1, "Thịt vai heo rửa sạch ráo, thái thành các lát mỏng vừa ăn rồi đem ướp cùng mật ong sả băm dầu hào tỏi băm trong 30 phút để ngấm sâu gia vị thơm lừng.", "", 180);
            step1.getStepIngredients().add(new Ingredient("400g", "Thịt nạc vai heo"));
            step1.getStepIngredients().add(new Ingredient("2 muỗng", "Sả băm"));
            step1.getStepIngredients().add(new Ingredient("1 muỗng", "Mật ong nguyên chất"));
            steps.add(step1);

            RecipeStep step2 = new RecipeStep(2, "Xiên thịt heo vào que xiên tre rồi đem nướng chín đều trên lửa than hoa hồng rực hoặc lò nướng nồi chiên không dầu đến khi sém vàng thơm phức chảy mỡ.", "", 450);
            step2.getStepIngredients().add(new Ingredient("400g", "Thịt nạc vai heo"));
            steps.add(step2);

            RecipeStep step3 = new RecipeStep(3, "Rạch một đường dọc sườn ổ bánh mì nóng giòn tan rồi quết bơ và pate mỏng ở đáy béo ngậy nếu thích.", "", 60);
            step3.getStepIngredients().add(new Ingredient("2 ổ", "Bánh mì giòn nóng"));
            steps.add(step3);

            RecipeStep step4 = new RecipeStep(4, "Xếp dưa leo lát dọc, kẹp xiên thịt heo nướng nóng hổi ngọt mềm, thêm rổ đồ chua giòn rụm đu đủ cà rốt ngâm dấm đường và cọng ngò rí tươi ngon lên trên cùng.", "", 90);
            step4.getStepIngredients().add(new Ingredient("2 ổ", "Bánh mì giòn nóng"));
            step4.getStepIngredients().add(new Ingredient("Vừa đủ", "Dưa leo, đồ chua, ngò rí"));
            steps.add(step4);

            r.setSteps(steps);

        } else if (name.contains("bánh xèo") || name.contains("banh xeo")) {
            r.setServings(4);
            r.setDifficulty("Trung bình");
            r.setDescription("Chiếc bánh xèo giòn rụm ngập sắc vàng bột nghệ miền Tây thơm phức bùi ngậy nước cốt dừa, cuộn đầy ắp nhân tôm tươi, thịt heo xào thơm lừng và giá đỗ ngọt thanh.");
            
            List<Ingredient> ingredients = new ArrayList<>();
            ingredients.add(new Ingredient("300g", "Bột bánh xèo mỏng"));
            ingredients.add(new Ingredient("200g", "Thịt ba chỉ thái mỏng"));
            ingredients.add(new Ingredient("200g", "Tôm tươi làm sạch"));
            ingredients.add(new Ingredient("150ml", "Nước cốt dừa thơm béo"));
            ingredients.add(new Ingredient("200g", "Giá đỗ sạch"));
            ingredients.add(new Ingredient("Vừa đủ", "Nước mắm tỏi ớt, rau cải xanh, xà lách"));
            r.setIngredients(ingredients);

            List<RecipeStep> steps = new ArrayList<>();

            RecipeStep step1 = new RecipeStep(1, "Hòa tan bột bánh xèo bột nghệ củ cùng nước ấm và nước cốt dừa béo bùi, thêm hành lá cắt khúc xắt nhuyễn khuấy tan mịn đều nghỉ 15 phút.", "", 180);
            step1.getStepIngredients().add(new Ingredient("300g", "Bột bánh xèo mỏng"));
            step1.getStepIngredients().add(new Ingredient("150ml", "Nước cốt dừa thơm béo"));
            steps.add(step1);

            RecipeStep step2 = new RecipeStep(2, "Xào săn chín tôm ngọt thịt ba chỉ thái lát mỏng trong chảo cùng tỏi băm thơm nồng rồi nêm nếm gia vị bùi bùi dọn ra dĩa.", "", 240);
            step2.getStepIngredients().add(new Ingredient("200g", "Thịt ba chỉ thái mỏng"));
            step2.getStepIngredients().add(new Ingredient("200g", "Tôm tươi làm sạch"));
            steps.add(step2);

            RecipeStep step3 = new RecipeStep(3, "Láng một lớp dầu mỏng lướt quanh mặt chảo lòng sâu thật nóng rồi múc gáo bột tráng mỏng xèo giòn kêu tanh tách. Đậy nắp đun 2 phút đến khi bột vàng giòn rìa bánh.", "", 120);
            step3.getStepIngredients().add(new Ingredient("300g", "Bột bánh xèo mỏng"));
            steps.add(step3);

            RecipeStep step4 = new RecipeStep(4, "Bày tôm xào, thịt heo chín, giá đỗ giòn ngọt lên một góc bánh rồi gập đôi bánh lại giòn tan, lấy bánh xèo nóng hổi ra dĩa cuốn bánh tráng rau sống ngon ngọt.", "", 90);
            step4.getStepIngredients().add(new Ingredient("200g", "Giá đỗ sạch"));
            steps.add(step4);

            r.setSteps(steps);

        } else if (name.contains("gỏi cuốn") || name.contains("goi cuon")) {
            r.setServings(3);
            r.setDifficulty("Dễ");
            r.setDescription("Món ăn thanh mát quốc hồn quốc túy đứng top thế giới. Sự kết hợp lý tưởng giữa tôm đỏ au luộc mọng nước, thịt luộc thái mỏng giòn ngọt cuộn dưa leo hẹ tươi mát dẻo thơm.");
            
            List<Ingredient> ingredients = new ArrayList<>();
            ingredients.add(new Ingredient("300g", "Tôm tươi căng mẩy"));
            ingredients.add(new Ingredient("300g", "Thịt ba chỉ heo giòn"));
            ingredients.add(new Ingredient("1 xấp", "Bánh tráng dẻo"));
            ingredients.add(new Ingredient("200g", "Bún tươi tươi sợi nhỏ"));
            ingredients.add(new Ingredient("Vừa đủ", "Rau xà lách, lá hẹ xanh, rau thơm"));
            ingredients.add(new Ingredient("Vừa đủ", "Tương hột bơ đậu phộng mặn ngọt"));
            r.setIngredients(ingredients);

            List<RecipeStep> steps = new ArrayList<>();

            RecipeStep step1 = new RecipeStep(1, "Luộc tôm mẩy tươi cùng thịt ba chỉ trong nước sôi bỏ xíu muối gừng thái lát mỏng chín mềm rồi vớt ra xả nước đá lạnh giòn dai cắt đôi tôm thái mỏng thịt.", "", 300);
            step1.getStepIngredients().add(new Ingredient("300g", "Tôm tươi căng mẩy"));
            step1.getStepIngredients().add(new Ingredient("300g", "Thịt ba chỉ heo giòn"));
            steps.add(step1);

            RecipeStep step2 = new RecipeStep(2, "Pha nước chấm bơ đậu phộng: Xào tương hột với tỏi phi thơm, thêm bơ đậu phộng ngầy ngậy, đường cát, ớt tươi giã nát cay tê hấp dẫn.", "", 180);
            step2.getStepIngredients().add(new Ingredient("Vừa đủ", "Tương hột bơ đậu phộng mặn ngọt"));
            steps.add(step2);

            RecipeStep step3 = new RecipeStep(3, "Làm ướt bánh tráng dẻo dai rồi xếp phẳng lỳ trên mâm. Trải xà lách, rau thơm ngò rí, đặt nhúm bún tươi sợi mỏng lên.", "", 90);
            step3.getStepIngredients().add(new Ingredient("1 xấp", "Bánh tráng dẻo"));
            step3.getStepIngredients().add(new Ingredient("200g", "Bún tươi tươi sợi nhỏ"));
            steps.add(step3);

            RecipeStep step4 = new RecipeStep(4, "Cuộn bánh tráng một vòng, đặt lát thịt chín luộc dải tôm đỏ rực rỡ hướng mặt đỏ ra ngoài, kẹp hẹ xanh ló ngọn rồi cuộn tròn chặt tay lôi cuốn vị giác.", "", 120);
            step4.getStepIngredients().add(new Ingredient("300g", "Tôm tươi căng mẩy"));
            step4.getStepIngredients().add(new Ingredient("300g", "Thịt ba chỉ heo giòn"));
            steps.add(step4);

            r.setSteps(steps);

        } else {
            // Default generic fallback
            r.setServings(4);
            r.setDifficulty("Dễ");
            r.setDescription("Công thức món ngon Việt Nam thuần túy, dễ làm tại nhà cho mâm cơm ấm cúng thơm tho chan chứa yêu thương gia đình Việt.");
            
            List<Ingredient> ingredients = new ArrayList<>();
            ingredients.add(new Ingredient("400g", "Nguyên liệu chính (Tôm/Thịt/Bò)"));
            ingredients.add(new Ingredient("2 củ", "Hành tím & tỏi thơm"));
            ingredients.add(new Ingredient("Vừa đủ", "Gia vị mắm muối đường tiêu ớt"));
            ingredients.add(new Ingredient("Vừa đủ", "Rau thơm ăn kèm tươi ngon"));
            r.setIngredients(ingredients);

            List<RecipeStep> steps = new ArrayList<>();
            RecipeStep s1 = new RecipeStep(1, "Sơ chế làm sạch các nguyên liệu tươi sống, để thật ráo nước và thái lát miếng mỏng vừa ăn.", "", 180);
            s1.getStepIngredients().add(new Ingredient("400g", "Nguyên liệu chính (Tôm/Thịt/Bò)"));
            steps.add(s1);

            RecipeStep s2 = new RecipeStep(2, "Ướp thịt cùng tỏi hành tím băm nước mắm hạt nêm trong 15 phút cho đậm hương vị thấm đẫm.", "", 240);
            s2.getStepIngredients().add(new Ingredient("2 củ", "Hành tím & tỏi thơm"));
            steps.add(s2);

            RecipeStep s3 = new RecipeStep(3, "Đun chảo nóng xào đảo đều tay lớn lửa thơm nồng nàn đến khi chín mềm chín tới mọng ngọt ngào.", "", 150);
            s3.getStepIngredients().add(new Ingredient("Vừa đủ", "Gia vị mắm muối đường tiêu ớt"));
            steps.add(s3);

            RecipeStep s4 = new RecipeStep(4, "Bày món ăn ra dĩa sâu lòng rắc tiêu xay và hành lá ngò rí lên thưởng thức cùng bát cơm trắng dẻo.", "", 60);
            s4.getStepIngredients().add(new Ingredient("Vừa đủ", "Rau thơm ăn kèm tươi ngon"));
            steps.add(s4);

            r.setSteps(steps);
        }

        return r;
    }
}
