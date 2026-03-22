package DAJ2EE.demo.config;

import DAJ2EE.demo.entity.Invoice;
import DAJ2EE.demo.entity.Payment;
import DAJ2EE.demo.entity.Role;
import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.repository.InvoiceRepository;
import DAJ2EE.demo.repository.PaymentRepository;
import DAJ2EE.demo.repository.RoleRepository;
import DAJ2EE.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initAdminUser(
            UserRepository userRepository,
            RoleRepository roleRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            // Đảm bảo roles tồn tại
            Role adminRole = roleRepository.findByName("ROLE_ADMIN");
            if (adminRole == null) {
                adminRole = new Role();
                adminRole.setName("ROLE_ADMIN");
                adminRole = roleRepository.save(adminRole);
            }

            Role tenantRole = roleRepository.findByName("ROLE_TENANT");
            if (tenantRole == null) {
                tenantRole = new Role();
                tenantRole.setName("ROLE_TENANT");
                tenantRole = roleRepository.save(tenantRole);
            }

            // Tạo hoặc cập nhật admin với mật khẩu BCrypt đúng
            // Tạo hoặc cập nhật admin với mật khẩu BCrypt đúng
            User admin = userRepository.findByUsername("admin").orElse(new User());
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setFullName("Phạm Thị Ái Ngân");
            admin.setEmail("ngan.admin@gmail.com");
            admin.setRole(adminRole);
            admin.setStatus(1);
            userRepository.save(admin);

            // Tạo Tenant user mẫu
            User tenant = userRepository.findByUsername("tenant").orElse(new User());
            tenant.setUsername("tenant");
            tenant.setPassword(passwordEncoder.encode("123456"));
            tenant.setFullName("Nguyễn Văn Tenant");
            tenant.setRole(tenantRole);
            tenant.setStatus(1);
            userRepository.save(tenant);

            // Khởi tạo Invoices & Payments mẫu nếu chưa có
            if (invoiceRepository.count() == 0) {
                Invoice inv1 = Invoice.builder()
                        .user(tenant)
                        .roomName("A-101")
                        .totalAmount(3500000.0)
                        .paidAmount(3500000.0)
                        .month("02/2026")
                        .status("PAID")
                        .dueDate(LocalDate.of(2026, 2, 10))
                        .build();
                invoiceRepository.save(inv1);

                paymentRepository.save(Payment.builder()
                        .invoice(inv1)
                        .amount(3500000.0)
                        .paymentDate(LocalDateTime.now().minusDays(30))
                        .method("Bank Transfer")
                        .build());

                Invoice inv2 = Invoice.builder()
                        .user(tenant)
                        .roomName("A-101")
                        .totalAmount(3500000.0)
                        .paidAmount(1000000.0)
                        .month("03/2026")
                        .status("PARTIAL")
                        .dueDate(LocalDate.of(2026, 3, 10))
                        .build();
                invoiceRepository.save(inv2);

                paymentRepository.save(Payment.builder()
                        .invoice(inv2)
                        .amount(1000000.0)
                        .paymentDate(LocalDateTime.now().minusDays(5))
                        .method("Momo")
                        .build());

                Invoice inv3 = Invoice.builder()
                        .user(tenant)
                        .roomName("A-101")
                        .totalAmount(3500000.0)
                        .paidAmount(0.0)
                        .month("04/2026")
                        .status("UNPAID")
                        .dueDate(LocalDate.of(2026, 4, 10))
                        .build();
                invoiceRepository.save(inv3);
            }

            System.out.println("✅ Admin user đã được khởi tạo/cập nhật với mật khẩu BCrypt chuẩn.");
        };
    }
}
