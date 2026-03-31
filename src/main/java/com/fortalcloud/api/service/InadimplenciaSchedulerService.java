package com.fortalcloud.api.service;

import com.fortalcloud.api.model.User;
import com.fortalcloud.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InadimplenciaSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(InadimplenciaSchedulerService.class);

    private final UserRepository userRepository;
    private final WasabiService wasabiService;

    public InadimplenciaSchedulerService(UserRepository userRepository, WasabiService wasabiService) {
        this.userRepository = userRepository;
        this.wasabiService = wasabiService;
    }

    //Bloqueia toda hora --- quem está inadimplente há mais de 10 dias
    @Scheduled(cron = "0 0 * * * *")
    public void blockInadimplecies(){
        LocalDateTime limit =  LocalDateTime.now().minusDays(10);

        List<User> usuarios = userRepository
                .findByPaymentFailedAtBeforeAndSubscriptionStatus(limit, "ACTIVE");


        for(User user : usuarios){
            user.setSubscriptionStatus("INACTIVE");
            userRepository.save(user);
            log.warn("🔒 Usuário bloqueado por inadimplência: {}", user.getEmail());
        }

        if(!usuarios.isEmpty()){
            log.info("Total desbloqueado neste ciclo: {}", usuarios.size());
        }
    }

    public void deleteInadimplencyFiles(){
        LocalDateTime limit = LocalDateTime.now().minusDays(12);

        List<User> usuarios = userRepository
                .findByPaymentFailedAtBeforeAndSubscriptionStatus(limit, "INACTIVE");

        for (User user : usuarios) {
            try{
                wasabiService.deleteBucket(user.getEmail());
                user.setPaymentFailedAt(null);
                userRepository.save(user);
                log.warn("🗑️ Arquivos deletados por inadimplência: {}", user.getEmail());
            } catch (Exception e){
                log.error("❌ Erro ao deletar bucket de {}: {}", user.getEmail(), e.getMessage());
            }
        }
    }
}
