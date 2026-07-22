package br.com.clinicaleve.appointment.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageQueueScheduler {

    private final MessageQueueProcessor processor;

    @Scheduled(fixedDelayString = "${app.messaging.queue-delay-ms:60000}")
    public void processDue() {
        processor.dueIds().forEach(processor::process);
    }
}
