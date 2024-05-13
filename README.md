# Amoji

**Adds emojis to Minecraft.**
![2024-05-13_19 36 57](https://github.com/albarv340/amoji/assets/54350887/df56bce6-287e-435a-bb47-fdf940828313)

This mod is based largely on [Emojiful](https://github.com/InnovativeOnlineIndustries/Emojiful), but with a focus on simplicity and making it easier to add custom emojis.

The functionality is intended to be kept to a bare minimum, to make maintenance easier.

## Adding your own emojis

There is a simple way of adding your own sets of emojis. Using the command `/amoji add <name> <url>` you can add a set of your own custom emojis.

The URL provided should return data of the following format:
```json
{
  "emoji1Name": "image1Url",
  "emoji2Name": "image2Url",
  "emoji3Name": "image3Url"
}
```

Example:
```json
{
  "like": "https://raw.githubusercontent.com/albarv340/amoji/main/like.png"
}
```
This example can be added with the command:

```
/amoji add example https://raw.githubusercontent.com/albarv340/amoji/main/example.json
```

The image URLs can be pretty much any static image URLs, and none of the files used have to be hosted on GitHub, it is just an example.

### Using Google Sheets

A relatively user-friendly emoji API can be created using google sheets. 

1. [Make a copy of this Google Sheet](https://docs.google.com/spreadsheets/d/1pXLQzADB58keQI1CyicETZN4ZuhrzDtWbbvJa3Y8ADA/edit?newcopy=true) (By clicking `File > Make a copy`)

2. Open the Apps Script editor by clicking `Extensions > Apps Script`.

3. Click `Deploy > New Deployment`. Fill in the relevant information and choose `Anyone` for who has access. And click `Deploy`.

4. Authorize Apps Script with your Google Account. (If it tells you the website is unsafe, you should still proceed, it is an official Google product, nothing to worry about).

5. Copy the link, visiting it should yield a JSON of the correct format with the emojis added to the sheet.

6. The emoji API can then be added to the mod with the command: 
```
/amoji add <name> <url>
```
