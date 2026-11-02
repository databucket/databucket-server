package pl.databucket.server.dto.validation

import jakarta.validation.Validation
import jakarta.validation.Validator
import pl.databucket.server.dto.BucketDto
import pl.databucket.server.dto.DataEnumDto
import pl.databucket.server.dto.DataEnumItemDto
import pl.databucket.server.dto.IconDto
import pl.databucket.server.dto.SvgDto
import spock.lang.Specification
import spock.lang.Unroll

class SvgCheckSpec extends Specification {

    @Unroll
    def "should accept the icon #description"() {
        expect:
        SvgCheck.problem(svg) == null

        where:
        description                 | svg
        "without content"           | null
        "content only (old format)" | '<path d="M12 2 2 22h20z"/>'
        "content of several shapes" | '<path d="M0 0h24v24H0z" fill="none"/><circle cx="12" cy="12" r="4"/>'
        "of any size"               | '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512"><path d="M1 1"/></svg>'
        "drawn with strokes"        | '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="1" y1="1" x2="9" y2="9"/></svg>'
        "with a gradient"           | '<svg viewBox="0 0 16 16"><defs><linearGradient id="g"><stop offset="0"/></linearGradient></defs><rect width="9" height="9" fill="url(#g)"/></svg>'
        "with a quoted inner url()" | '<svg><defs><linearGradient id="g"/></defs><rect style="fill: url(\'#g\')" clip-path="url( &quot;#g&quot; )"/></svg>'
        "with an inner reference"   |'<svg xmlns:xlink="http://www.w3.org/1999/xlink"><symbol id="s"/><use xlink:href="#s"/></svg>'
        "with a title"              | '<svg><title>Star</title><path d="M1 1"/></svg>'
    }

    @Unroll
    def "should reject the icon #description"() {
        expect:
        SvgCheck.problem(svg)?.startsWith(problem)

        where:
        description                 | svg                                                                    || problem
        "with a script"             | '<script>alert(1)</script>'                                            || "The icon contains a forbidden element: script"
        "with a style"              | '<svg><style>body{display:none}</style></svg>'                         || "The icon contains a forbidden element: style"
        "with a link"               | '<a href="javascript:alert(1)"><path d="M1 1"/></a>'                   || "The icon contains a forbidden element: a"
        "with foreign content"      | '<foreignObject><div>x</div></foreignObject>'                          || "The icon contains a forbidden element: foreignObject"
        "with an image"             | '<image href="https://example.com/t.png"/>'                            || "The icon contains a forbidden element: image"
        "with an event handler"     | '<path onclick="alert(1)" d="M1 1"/>'                                  || "The icon contains a forbidden attribute: onclick"
        "with an external use"      | '<use href="https://example.com/i.svg#x"/>'                            || "The icon refers to an external resource: href"
        "with an external url()"    | '<path style="fill:url(https://example.com/x)" d="M1 1"/>'             || "The icon refers to an external resource: style"
        "with a quoted external url()" | '<path style="fill:url(\'https://example.com/x\')" d="M1 1"/>'       || "The icon refers to an external resource: style"
        "with javascript in a value"|'<path fill="java&#10;script:x" d="M1 1"/>'                            || "The icon refers to an external resource: fill"
        "with a DOCTYPE (entities)" | '<!DOCTYPE svg [<!ENTITY x "y">]><svg>&x;</svg>'                       || "The icon is not valid SVG"
        "that is not XML"           | '<path d="M1 1">'                                                      || "The icon is not valid SVG"
        "that is not SVG"           | '<svg:html xmlns:svg="x"/>'                                            || "The icon is not"
        "larger than 32 KB"         | '<path d="' + 'M1 1'.repeat(9000) + '"/>'                              || "The icon is larger than 32 KB"
    }

    def "should check the icons of the icon library, buckets and enum items"() {
        given:
        def validator = Validation.buildDefaultValidatorFactory().validator
        def bad = '<svg><style>*{}</style></svg>'

        expect:
        violations(validator, new SvgDto(name: "x", structure: bad)) == ["structure": "The icon contains a forbidden element: style"]
        violations(validator, new BucketDto(name: "bucket", icon: new IconDto("x", null, bad))) == ["icon.svg": "The icon contains a forbidden element: style"]
        violations(validator, new DataEnumDto(name: "enum", items: [new DataEnumItemDto(value: "v", icon: new IconDto("x", null, bad))])) == ["items[0].icon.svg": "The icon contains a forbidden element: style"]
        violations(validator, new BucketDto(name: "bucket", icon: new IconDto("x", null, '<path d="M1 1"/>'))).isEmpty()
    }

    static Map<String, String> violations(Validator validator, Object object) {
        validator.validate(object).collectEntries { [(it.propertyPath.toString()): it.message] }
    }
}
