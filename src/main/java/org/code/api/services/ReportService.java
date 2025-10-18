package org.code.api.services;

import org.code.api.domain.ports.ReportPort;
import org.springframework.stereotype.Service;

@Service
public class ReportService implements ReportPort {

    @Override
    public boolean makeReportRequest(String name) {
        return false;
    }
}
