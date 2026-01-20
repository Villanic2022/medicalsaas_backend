package com.saas.medical.config;

import com.saas.medical.model.entity.*;
import com.saas.medical.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
@Slf4j
@Profile("data-loader") // Solo activar con perfil específico
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final SpecialtyRepository specialtyRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final ProfessionalRepository professionalRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public ApplicationRunner initializeData() {
        return args -> {
            try {
                log.info("🚀 INICIANDO CARGA DE DATOS DE DESARROLLO...");

                // 1. Crear roles si no existen
                log.info("📋 Creando roles...");
                createRolesIfNotExist();

                // 2. Crear especialidades si no existen
                log.info("🩺 Creando especialidades...");
                createSpecialtiesIfNotExist();

                // 3. Crear tenant de ejemplo
                log.info("🏥 Creando tenant demo...");
                Tenant demoTenant = createDemoTenantIfNotExist();

                // 4. Crear usuarios de ejemplo
                log.info("👥 Creando usuarios demo...");
                createDemoUsersIfNotExist(demoTenant);

                // 5. Crear profesionales de ejemplo
                log.info("👨‍⚕️ Creando profesionales demo...");
                createDemoProfessionalsIfNotExist(demoTenant);

                // Log de verificación final
                log.info("📊 VERIFICANDO DATOS CARGADOS:");
                log.info("   - Roles: {}", roleRepository.count());
                log.info("   - Especialidades: {}", specialtyRepository.count());
                log.info("   - Tenants: {}", tenantRepository.count());
                log.info("   - Usuarios: {}", userRepository.count());
                log.info("   - Profesionales: {}", professionalRepository.count());

                log.info("✅ DATOS DE DESARROLLO INICIALIZADOS CORRECTAMENTE");
            } catch (Exception e) {
                log.error("❌ ERROR AL INICIALIZAR DATOS: {}", e.getMessage(), e);
                throw e;
            }
        };
    }

    private void createRolesIfNotExist() {
        if (roleRepository.count() == 0) {
            Role adminRole = new Role();
            adminRole.setName("ADMIN");
            adminRole.setDescription("Administrador del sistema");
            roleRepository.save(adminRole);

            Role ownerRole = new Role();
            ownerRole.setName("OWNER");
            ownerRole.setDescription("Propietario del consultorio");
            roleRepository.save(ownerRole);

            Role staffRole = new Role();
            staffRole.setName("STAFF");
            staffRole.setDescription("Personal del consultorio");
            roleRepository.save(staffRole);

            log.info("Roles creados: ADMIN, OWNER, STAFF");
        }
    }

    private void createSpecialtiesIfNotExist() {
        if (specialtyRepository.count() == 0) {
            String[] specialties = {
                "Medicina General", "Cardiología", "Dermatología", "Ginecología",
                "Pediatría", "Traumatología", "Oftalmología", "Odontología",
                "Neurología", "Psiquiatría", "Endocrinología", "Urología"
            };

            for (String name : specialties) {
                Specialty specialty = new Specialty();
                specialty.setName(name);
                specialty.setDescription("Especialidad de " + name);
                specialty.setActive(true);
                specialtyRepository.save(specialty);
            }

            log.info("Especialidades creadas: {}", specialties.length);
        }
    }

    private Tenant createDemoTenantIfNotExist() {
        // Crear tenant demo original
        Tenant demoTenant = tenantRepository.findBySlug("demo-clinic")
                .orElseGet(() -> {
                    Tenant tenant = new Tenant();
                    tenant.setName("Clínica Demo");
                    tenant.setSlug("demo-clinic");
                    tenant.setEmail("contacto@demo-clinic.com");
                    tenant.setPhone("011-4444-5555");
                    tenant.setAddress("Av. Corrientes 1234, CABA");
                    tenant.setCity("Buenos Aires");
                    tenant.setTimezone("America/Argentina/Buenos_Aires");
                    tenant.setAppointmentDurationMinutes(30);
                    tenant.setActive(true);

                    tenant = tenantRepository.save(tenant);
                    log.info("Tenant demo creado: {}", tenant.getName());
                    return tenant;
                });

        // Crear tenant consultorio dental
        tenantRepository.findBySlug("consultorio-dental")
                .orElseGet(() -> {
                    Tenant tenant = new Tenant();
                    tenant.setName("Consultorio Dental Villa");
                    tenant.setSlug("consultorio-dental");
                    tenant.setEmail("contacto@consultorio-dental.com");
                    tenant.setPhone("011-5555-6666");
                    tenant.setAddress("Av. Santa Fe 2500, CABA");
                    tenant.setCity("Buenos Aires");
                    tenant.setTimezone("America/Argentina/Buenos_Aires");
                    tenant.setAppointmentDurationMinutes(45);
                    tenant.setActive(true);

                    tenant = tenantRepository.save(tenant);
                    log.info("Tenant consultorio dental creado: {}", tenant.getName());
                    return tenant;
                });

        return demoTenant;
    }

    private void createDemoUsersIfNotExist(Tenant tenant) {
        // Usuario admin global
        if (!userRepository.existsByEmail("admin@medical-saas.com")) {
            Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();

            User admin = new User();
            admin.setEmail("admin@medical-saas.com");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setFirstName("Admin");
            admin.setLastName("System");
            admin.setActive(true);
            admin.setEmailVerified(true);
            admin.setRoles(Set.of(adminRole));

            userRepository.save(admin);
            log.info("Usuario admin creado: admin@medical-saas.com / admin123");
        }

        // Usuario owner del demo clinic
        if (!userRepository.existsByEmail("owner@demo-clinic.com")) {
            Role ownerRole = roleRepository.findByName("OWNER").orElseThrow();

            User owner = new User();
            owner.setTenantId(tenant.getId());
            owner.setEmail("owner@demo-clinic.com");
            owner.setPasswordHash(passwordEncoder.encode("owner123"));
            owner.setFirstName("Dr. Juan");
            owner.setLastName("Pérez");
            owner.setActive(true);
            owner.setEmailVerified(true);
            owner.setRoles(Set.of(ownerRole));

            userRepository.save(owner);
            log.info("Usuario owner creado: owner@demo-clinic.com / owner123");
        }

        // Usuario staff del demo clinic
        if (!userRepository.existsByEmail("staff@demo-clinic.com")) {
            Role staffRole = roleRepository.findByName("STAFF").orElseThrow();

            User staff = new User();
            staff.setTenantId(tenant.getId());
            staff.setEmail("staff@demo-clinic.com");
            staff.setPasswordHash(passwordEncoder.encode("staff123"));
            staff.setFirstName("María");
            staff.setLastName("González");
            staff.setActive(true);
            staff.setEmailVerified(true);
            staff.setRoles(Set.of(staffRole));

            userRepository.save(staff);
            log.info("Usuario staff creado: staff@demo-clinic.com / staff123");
        }
    }

    private void createDemoProfessionalsIfNotExist(Tenant tenant) {
        if (professionalRepository.findByTenantIdAndActive(tenant.getId(), true).isEmpty()) {
            Specialty medicinaGeneral = specialtyRepository.findByNameAndActive("Medicina General", true)
                    .orElseThrow(() -> new RuntimeException("Medicina General specialty not found"));

            Specialty cardiologia = specialtyRepository.findByNameAndActive("Cardiología", true)
                    .orElseThrow(() -> new RuntimeException("Cardiología specialty not found"));

            // Profesional 1: Dr. Juan Pérez - Medicina General
            Professional prof1 = new Professional();
            prof1.setTenantId(tenant.getId());
            prof1.setSpecialty(medicinaGeneral);
            prof1.setFirstName("Dr. Juan");
            prof1.setLastName("Pérez");
            prof1.setLicenseNumber("MP-12345");
            prof1.setEmail("juan.perez@demo-clinic.com");
            prof1.setPhone("011-5555-1234");
            prof1.setBio("Especialista en medicina general con 15 años de experiencia");
            prof1.setActive(true);
            professionalRepository.save(prof1);

            // Profesional 2: Dra. Ana García - Cardiología
            Professional prof2 = new Professional();
            prof2.setTenantId(tenant.getId());
            prof2.setSpecialty(cardiologia);
            prof2.setFirstName("Dra. Ana");
            prof2.setLastName("García");
            prof2.setLicenseNumber("MP-67890");
            prof2.setEmail("ana.garcia@demo-clinic.com");
            prof2.setPhone("011-5555-5678");
            prof2.setBio("Cardióloga especialista en prevención cardiovascular");
            prof2.setActive(true);
            professionalRepository.save(prof2);

            log.info("Profesionales demo creados: 2 profesionales");
        }
    }
}
