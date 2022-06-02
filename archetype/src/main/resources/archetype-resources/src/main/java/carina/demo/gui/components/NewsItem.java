#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.carina.demo.gui.components;

import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import com.qaprosoft.carina.core.foundation.webdriver.decorator.ExtendedWebElement;
import com.qaprosoft.carina.core.gui.AbstractUIObject;

public class NewsItem extends AbstractUIObject {

    @FindBy(xpath="./a")
    public ExtendedWebElement titleLink;
    
    public NewsItem(WebDriver driver, SearchContext sc) {
        super(driver, sc);
    }
    
    public String readTitle() {
        return titleLink.getElement().getText();
    }
}
