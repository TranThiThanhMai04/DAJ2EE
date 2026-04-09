package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.Contract;

public interface ContractNotificationDispatcher {
    void notifyContractCreated(Contract contract);
}