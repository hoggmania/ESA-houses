package com.example.dashboard;

import com.example.dashboard.exception.ValidationException;
import com.example.dashboard.model.ESA;
import com.example.dashboard.model.Governance;
import com.example.dashboard.model.Capabilities;
import com.example.dashboard.model.Domain;
import com.example.dashboard.model.ComponentItem;
import com.example.dashboard.service.SvgService;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class ValidationTest {

    @Inject
    SvgService svgService;

    @Test
    public void testValidESAPassesValidation() {
        ESA esa = createValidESA();
        assertDoesNotThrow(() -> ESA.validateESA(esa));
    }

    @Test
    public void testNullESAThrowsValidationException() {
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> ESA.validateESA(null));
        assertTrue(exception.getMessage().contains("ESA root object cannot be null"));
    }

    @Test
    public void testMissingStatusEnumThrowsValidationException() {
        ESA esa = createValidESA();
        esa.governance.components.get(0).status = null;
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> ESA.validateESA(esa));
        assertTrue(exception.getMessage().contains("status"));
        assertTrue(exception.getMessage().contains("NOT_EXISTING, LOW, MEDIUM, HIGH, EFFECTIVE"));
    }

    @Test
    public void testMissingMaturityEnumThrowsValidationException() {
        ESA esa = createValidESA();
        esa.governance.components.get(0).maturity = null;
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> ESA.validateESA(esa));
        assertTrue(exception.getMessage().contains("maturity"));
        assertTrue(exception.getMessage().contains("NOT_EXISTING, INITIAL, REPEATABLE, DEFINED, MANAGED, OPTIMISED"));
    }

    @Test
    public void testMissingNameThrowsValidationException() {
        ESA esa = createValidESA();
        esa.governance.components.get(0).name = null;
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> ESA.validateESA(esa));
        assertTrue(exception.getMessage().contains("missing required field: name"));
    }

    @Test
    public void testEmptyNameThrowsValidationException() {
        ESA esa = createValidESA();
        esa.governance.components.get(0).name = "   ";
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> ESA.validateESA(esa));
        assertTrue(exception.getMessage().contains("missing required field: name"));
    }

    @Test
    public void testNegativeInitiativesThrowsValidationException() {
        ESA esa = createValidESA();
        esa.governance.components.get(0).initiatives = -5;
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> ESA.validateESA(esa));
        assertTrue(exception.getMessage().contains("invalid initiatives count"));
        assertTrue(exception.getMessage().contains("-5"));
    }

    @Test
    public void testValidationInDomainComponents() {
        ESA esa = createValidESA();
        esa.capabilities.domains.get(0).components.get(0).status = null;
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> ESA.validateESA(esa));
        assertTrue(exception.getMessage().contains("status"));
    }

    @Test
    public void testNullComponentInGovernanceThrowsException() {
        ESA esa = createValidESA();
        esa.governance.components.add(null);
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> ESA.validateESA(esa));
        assertTrue(exception.getMessage().contains("cannot be null"));
    }

    @Test
    public void testNullDomainThrowsException() {
        ESA esa = createValidESA();
        esa.capabilities.domains.add(null);
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> ESA.validateESA(esa));
        assertTrue(exception.getMessage().contains("Domain at index"));
        assertTrue(exception.getMessage().contains("cannot be null"));
    }

    @Test
    public void testValidESAWithZeroInitiativesPassesValidation() {
        ESA esa = createValidESA();
        esa.governance.components.get(0).initiatives = 0;
        assertDoesNotThrow(() -> ESA.validateESA(esa));
    }

    @Test
    public void testValidESAWithDoubleBorderPassesValidation() {
        ESA esa = createValidESA();
        esa.governance.components.get(0).doubleBorder = true;
        assertDoesNotThrow(() -> ESA.validateESA(esa));
    }

    private ESA createValidESA() {
        ESA esa = new ESA();
        esa.title = "Test Dashboard";

        // Create governance
        Governance gov = new Governance();
        gov.title = "Test Governance";
        gov.components = new ArrayList<>();
        
        ComponentItem govComp = new ComponentItem();
        govComp.name = "Governance Item";
        govComp.capability = "SAST";
        govComp.status = ComponentItem.Status.HIGH;
        govComp.maturity = ComponentItem.Maturity.MANAGED;
        govComp.initiatives = 3;
        govComp.doubleBorder = false;
        govComp.rag = "green";
        gov.components.add(govComp);
        
        esa.governance = gov;

        // Create capabilities with domains
        Capabilities cap = new Capabilities();
        cap.title = "Test Capabilities";
        cap.domains = new ArrayList<>();
        
        Domain domain = new Domain();
        domain.domain = "Test Domain";
        domain.components = new ArrayList<>();
        
        ComponentItem domComp = new ComponentItem();
        domComp.name = "Domain Component";
        domComp.capability = "DAST";
        domComp.status = ComponentItem.Status.MEDIUM;
        domComp.maturity = ComponentItem.Maturity.DEFINED;
        domComp.initiatives = 0;
        domComp.doubleBorder = false;
        domComp.rag = "amber";
        domain.components.add(domComp);
        
        cap.domains.add(domain);
        esa.capabilities = cap;

        return esa;
    }
}
