package ru.ovsyannikov.parsing;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Date;

/**
 * @author Georgii Ovsiannikov
 * @since 4/26/15
 */
public class User {

    public static enum Gender {
        M("male"),
        F("female")

        ;

        private String description;

        Gender(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private Long userId;
    private String vkId;
    private String facebookId;
    private String twitterId;
    private String name;
    private String country;
    private String city;
    private Date birthday;
    private Gender gender;

    @JsonProperty("user_id")
    public Long getUserId() {
        return userId;
    }

    @JsonProperty("user_id")
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @JsonProperty("vk_id")
    public String getVkId() {
        return vkId;
    }

    @JsonProperty("vk_id")
    public void setVkId(String vkId) {
        this.vkId = vkId;
    }

    @JsonProperty("fb_id")
    public String getFacebookId() {
        return facebookId;
    }

    @JsonProperty("fb_id")
    public void setFacebookId(String facebookId) {
        this.facebookId = facebookId;
    }

    @JsonProperty("twitter_id")
    public String getTwitterId() {
        return twitterId;
    }

    @JsonProperty("twitter_id")
    public void setTwitterId(String twitterId) {
        this.twitterId = twitterId;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("country")
    public String getCountry() {
        return country;
    }

    @JsonProperty("country")
    public void setCountry(String country) {
        this.country = country;
    }

    @JsonProperty("city")
    public String getCity() {
        return city;
    }

    @JsonProperty("city")
    public void setCity(String city) {
        this.city = city;
    }

    @JsonProperty("birthday")
    public Date getBirthday() {
        return birthday;
    }

    @JsonProperty("birthday")
    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    @JsonProperty("gender")
    public Gender getGender() {
        return gender;
    }

    @JsonProperty("gender")
    public void setGender(String  gender) {
        this.gender = Gender.valueOf(gender);
    }
}
