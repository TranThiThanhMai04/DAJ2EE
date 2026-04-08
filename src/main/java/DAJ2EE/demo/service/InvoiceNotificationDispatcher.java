package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.Invoice;

public interface InvoiceNotificationDispatcher {
    void notifyInvoiceCreated(Invoice invoice);
}
