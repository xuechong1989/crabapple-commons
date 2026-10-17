package xc.crabapple.core.sort;

/**
 * 2018-10-31
 */
public final class Person {

    private Long id;
    private String name;
    private Integer age;

    public Person(final Long id, final String name, final Integer age){
        this.id = id;
        this.name = name;
        this.age = age;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(final Integer age) {
        this.age = age;
    }

}
