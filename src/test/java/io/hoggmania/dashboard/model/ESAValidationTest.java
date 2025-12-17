package io.hoggmania.dashboard.model;

import io.hoggmania.dashboard.exception.ValidationException;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class ESAValidationTest {

    @Test
    public void testValidateESA_NullRoot() {
        assertThrows(ValidationException.class, () -> ESA.validateESA(null));
    }

    @Test
    public void testValidateESA_ValidStructure() {
        ESA esa = createValidESA();
        assertDoesNotThrow(() -> ESA.validateESA(esa));
    }

    @Test
    public void testValidateESA_MissingStatus() {
        ESA esa = createValidESA();
        esa.governance.components.get(0).status = null;
        
        ValidationException ex = assertThrows(ValidationException.class, () -> ESA.validateESA(esa));
        assertTrue(ex.getMessage().contains("statusEnum"));
    }

    @Test
    public void testValidateESA_MissingMaturity() {
        ESA esa = createValidESA();
        esa.governance.components.get(0).maturity = null;
        
        ValidationException ex = assertThrows(ValidationException.class, () -> ESA.validateESA(esa));
        assertTrue(ex.getMessage().contains("maturityEnum"));
    }

    @Test
    public void testValidateESA_MissingName() {
        ESA esa = createValidESA();
        esa.governance.components.get(0).name = null;
        
        ValidationException ex = assertThrows(ValidationException.class, () -> ESA.validateESA(esa));
        assertTrue(ex.getMessage().contains("name"));
    }

    @Test
    public void testValidateESA_BlankName() {
        ESA esa = createValidESA();
        esa.governance.components.get(0).name = "   ";
        
        ValidationException ex = assertThrows(ValidationException.class, () -> ESA.validateESA(esa));
        assertTrue(ex.getMessage().contains("name"));
    }

    @Test
    public void testValidateESA_NegativeInitiatives() {
        ESA esa = createValidESA();
        esa.governance.components.get(0).initiatives = -1;
        
        ValidationException ex = assertThrows(ValidationException.class, () -> ESA.validateESA(esa));
        assertTrue(ex.getMessage().contains("initiatives"));
    }

    @Test
    public void testValidateESA_NullComponent() {
        ESA esa = createValidESA();
        esa.governance.components.add(null);
        
        assertThrows(ValidationException.class, () -> ESA.validateESA(esa));
    }

    @Test
    public void testValidateESA_ValidCapabilities() {
        ESA esa = createValidESA();
        Capabilities capabilities = new Capabilities();
        capabilities.title = "Test Capabilities";
        capabilities.domains = new ArrayList<>();
        
        Domain domain = new Domain();
        domain.domain = "Test Domain";
        domain.components = new ArrayList<>();
        domain.components.add(createValidComponent());
        capabilities.domains.add(domain);
        
        esa.capabilities = capabilities;
        
        assertDoesNotThrow(() -> ESA.validateESA(esa));
    }

    @Test
    public void testValidateESA_NullDomain() {
        ESA esa = createValidESA();
        Capabilities capabilities = new Capabilities();
        capabilities.title = "Test Capabilities";
        capabilities.domains = new ArrayList<>();
        capabilities.domains.add(null);
        esa.capabilities = capabilities;
        
        assertThrows(ValidationException.class, () -> ESA.validateESA(esa));
    }

    private ESA createValidESA() {
        ESA esa = new ESA();
        esa.title = "Test ESA";
        esa.icon = "shield";
        
        Governance governance = new Governance();
        governance.title = "Test Governance";
        governance.components = new ArrayList<>();
        governance.components.add(createValidComponent());
        esa.governance = governance;
        
        return esa;
    }

    private ComponentItem createValidComponent() {
        ComponentItem component = new ComponentItem();
        component.name = "Test Component";
        component.status = ComponentItem.Status.MEDIUM;
        component.maturity = ComponentItem.Maturity.DEFINED;
        component.initiatives = 0;
        return component;
    }
}

