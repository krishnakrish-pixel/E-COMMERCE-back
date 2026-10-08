package com.shophub.backend.config;

import com.shophub.backend.entity.Coupon;
import com.shophub.backend.entity.Product;
import com.shophub.backend.entity.User;
import com.shophub.backend.repository.CouponRepository;
import com.shophub.backend.repository.ProductRepository;
import com.shophub.backend.repository.UserRepository;
import com.shophub.backend.service.ProductService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs on every startup but only inserts data when it is missing:
 *  - the admin account (admin@shophub.com)
 *  - a demo customer (demo@shophub.com)
 *  - the 16 starter products from the original frontend (ids 1..16)
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final CouponRepository couponRepository;

    @Value("${shophub.admin.email}")
    private String adminEmail;
    @Value("${shophub.admin.password}")
    private String adminPassword;
    @Value("${shophub.demo.email}")
    private String demoEmail;
    @Value("${shophub.demo.password}")
    private String demoPassword;

    private final List<Product> seed = new ArrayList<>();

    public DataSeeder(UserRepository userRepository,
                      ProductRepository productRepository,
                      PasswordEncoder passwordEncoder,
                      CouponRepository couponRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
        this.couponRepository = couponRepository;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmailIgnoreCase(adminEmail)) {
            User admin = new User();
            admin.setEmail(adminEmail.toLowerCase());
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setFirstName("Administrator");
            admin.setLastName("Manager");
            admin.setRole("admin");
            admin.setPhone("+91 99999 88888");
            admin.setAddress("ShopHub Executive Tower, Level 14");
            admin.setCity("Bengaluru");
            admin.setZipCode("560001");
            admin.setCountry("India");
            userRepository.save(admin);
        }

        if (!userRepository.existsByEmailIgnoreCase(demoEmail)) {
            User demo = new User();
            demo.setEmail(demoEmail.toLowerCase());
            demo.setPassword(passwordEncoder.encode(demoPassword));
            demo.setFirstName("Alex");
            demo.setLastName("Sharma");
            demo.setRole("user");
            demo.setPhone("+91 98765 43210");
            demo.setAddress("42, Lotus Boulevard, Bandra West");
            demo.setCity("Mumbai");
            demo.setZipCode("400050");
            demo.setCountry("India");
            userRepository.save(demo);
        }

        if (productRepository.count() == 0) {
            buildProducts();
            productRepository.saveAll(seed);
        }

        // Databases created before stock tracking existed: give those products some stock (runs once, only for NULLs)
        List<Product> untracked = productRepository.findAll().stream().filter(p -> p.getStock() == null).toList();
        if (!untracked.isEmpty()) {
            untracked.forEach(p -> p.setStock(ProductService.DEFAULT_STOCK));
            productRepository.saveAll(untracked);
        }

        seedCoupons();
    }

    private void seedCoupons() {
        if (couponRepository.count() > 0) return;
        coupon("WELCOME10", "10% off your order", Coupon.PERCENT, "10", "0", null);
        coupon("SAVE100", "₹100 off orders above ₹999", Coupon.FIXED, "100", "999", null);
        coupon("FESTIVE20", "20% off orders above ₹2,000", Coupon.PERCENT, "20", "2000", 200);
    }

    private void coupon(String code, String description, String type, String value, String min, Integer maxUses) {
        Coupon c = new Coupon();
        c.setCode(code);
        c.setDescription(description);
        c.setDiscountType(type);
        c.setDiscountValue(new BigDecimal(value));
        c.setMinOrderAmount(new BigDecimal(min));
        c.setMaxUses(maxUses);
        c.setUsedCount(0);
        c.setActive(true);
        couponRepository.save(c);
    }

    private void add(String name, String price, String category, double rating, String image, String description) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(new BigDecimal(price));
        p.setCategory(category);
        p.setRating(rating);
        p.setImage(image);
        p.setDescription(description);
        p.setStock(ProductService.DEFAULT_STOCK);
        seed.add(p);
    }

    private void buildProducts() {
        add("Premium Wireless Headphones", "299.99", "Electronics", 4.5,
            "https://images.unsplash.com/photo-1695634463848-4db4e47703a4?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxtb2Rlcm4lMjB3aXJlbGVzcyUyMGhlYWRwaG9uZXMlMjB3aGl0ZXxlbnwxfHx8fDE3NzQ5NzE3NDh8MA&ixlib=rb-4.1.0&q=80&w=1080&utm_source=figma&utm_medium=referral",
            "High-quality wireless headphones with active noise cancellation, 40h battery life, and crystal-clear acoustic fidelity.");
        add("Luxury Silver Watch", "899.99", "Accessories", 5,
            "https://images.unsplash.com/photo-1634113709155-5c2756711384?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxsdXh1cnklMjB3cmlzdHdhdGNoJTIwc2lsdmVyfGVufDF8fHx8MTc3NDg4NTQ4OXww&ixlib=rb-4.1.0&q=80&w=1080&utm_source=figma&utm_medium=referral",
            "Elegant stainless steel timepiece with precision automatic movement, sapphire crystal, and sophisticated design.");
        add("Designer Sunglasses", "199.99", "Accessories", 4,
            "https://images.unsplash.com/photo-1760446032732-c042b0d43580?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxkZXNpZ25lciUyMHN1bmdsYXNzZXMlMjBibGFja3xlbnwxfHx8fDE3NzQ5NDkzNTN8MA&ixlib=rb-4.1.0&q=80&w=1080&utm_source=figma&utm_medium=referral",
            "Stylish lightweight sunglasses featuring 100% UV400 protection and glare-reducing polarized lenses.");
        add("Premium Leather Backpack", "249.99", "Bags", 4.5,
            "https://images.unsplash.com/photo-1594300418249-eebf1858e9b7?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxsZWF0aGVyJTIwYmFja3BhY2slMjBicm93bnxlbnwxfHx8fDE3NzQ5MTkzNTR8MA&ixlib=rb-4.1.0&q=80&w=1080&utm_source=figma&utm_medium=referral",
            "Handcrafted full-grain leather backpack with dedicated padded laptop compartment and water-resistant lining.");
        add("Minimalist White Sneakers", "129.99", "Fashion", 4.5,
            "https://images.unsplash.com/photo-1573875133340-0b589f59a8c4?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxzbmVha2VycyUyMHdoaXRlJTIwbWluaW1hbHxlbnwxfHx8fDE3NzQ5NzI4OTh8MA&ixlib=rb-4.1.0&q=80&w=1080&utm_source=figma&utm_medium=referral",
            "Clean, versatile, and breathable sneakers designed for all-day comfort and timeless modern style.");
        add("Flagship Smartphone Pro", "1299.99", "Mobiles", 5,
            "https://images.unsplash.com/photo-1618972888345-125ccf0fee12?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxzbWFydHBob25lJTIwYmxhY2slMjB0ZWNobm9sb2d5fGVufDF8fHx8MTc3NDk4MjUzMHww&ixlib=rb-4.1.0&q=80&w=1080&utm_source=figma&utm_medium=referral",
            "Next-gen flagship smartphone with OLED 120Hz display, pro triple-camera sensor, and lightning-fast 5G processor.");
        add("Professional Laptop 16-inch", "1899.99", "Laptops", 4.8,
            "https://images.unsplash.com/photo-1706735733956-deebaf5d001c?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxsYXB0b3AlMjBjb21wdXRlciUyMHdvcmtzcGFjZXxlbnwxfHx8fDE3NzQ5NTU0MjZ8MA&ixlib=rb-4.1.0&q=80&w=1080&utm_source=figma&utm_medium=referral",
            "High-performance powerhouse laptop with 32GB RAM, 1TB SSD, vibrant Liquid Retina display, and all-day battery efficiency.");
        add("Professional Cinema Camera", "2499.99", "Electronics", 5,
            "https://images.unsplash.com/photo-1729655669048-a667a0b01148?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxjYW1lcmElMjBwaG90b2dyYXBoeSUyMGVxdWlwbWVudHxlbnwxfHx8fDE3NzQ5MzUxODF8MA&ixlib=rb-4.1.0&q=80&w=1080&utm_source=figma&utm_medium=referral",
            "Cinema-grade mirrorless camera with 8K recording capability, in-body stabilization, and low-light sensor dynamics.");
        add("Ultra-Slim 5G Smartphone", "799.99", "Mobiles", 4.6,
            "https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?q=80&w=800&auto=format&fit=crop",
            "Feather-light smartphone with edge-to-edge HDR display, AI photography enhancements, and 68W fast charging.");
        add("Ultra-Thin Portable Ultrabook", "1149.99", "Laptops", 4.7,
            "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?q=80&w=800&auto=format&fit=crop",
            "Sleek aluminum body ultrabook weighing just 1.1kg, with silent cooling and 18-hour battery longevity.");
        add("Classic Vintage Denim Jacket", "189.99", "Fashion", 4.4,
            "https://images.unsplash.com/photo-1556821840-3a63f95609a7?q=80&w=800&auto=format&fit=crop",
            "Premium heavyweight cotton denim jacket with reinforced seams and timeless streetwear fit.");
        add("Modern Ambient Ceramic Lamp", "159.99", "Home & Living", 4.7,
            "https://images.unsplash.com/photo-1616046229478-9901c5536a45?q=80&w=800&auto=format&fit=crop",
            "Sculptural matte ceramic table lamp offering dimmable warm ambient light for bedrooms and living spaces.");
        add("Organic Botanical Skincare Serum", "89.99", "Beauty", 4.9,
            "https://images.unsplash.com/photo-1594035910387-fea47794261f?q=80&w=800&auto=format&fit=crop",
            "100% natural cold-pressed rejuvenating serum enriched with vitamin C, hyaluronic acid, and botanical oils.");
        add("Smart Fitness & Health Watch", "349.99", "Electronics", 4.6,
            "https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1?q=80&w=800&auto=format&fit=crop",
            "Real-time ECG, blood oxygen tracking, sleep staging, and water resistance up to 50 meters with customizable bands.");
        add("Canvas Weekender Travel Duffel", "169.99", "Bags", 4.5,
            "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?q=80&w=800&auto=format&fit=crop",
            "Heavy-duty water-repellent canvas duffel with genuine leather accents, shoe compartment, and padded shoulder strap.");
        add("Aroma Ultrasonic Diffuser", "69.99", "Home & Living", 4.8,
            "https://images.unsplash.com/photo-1608571423902-eed4a5ad8108?q=80&w=800&auto=format&fit=crop",
            "Whisper-quiet ultrasonic essential oil diffuser with soothing color-cycling LED ambient lighting.");
    }
}
