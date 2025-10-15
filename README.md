# Infinity Data

A program to convert unit data from Corvus Belli's Army Builder for the Infinity tabletop game.

Currently, the main function is to take an army code and generate nice, printable cards for all the units in the army.

If Corvus Belli provides an image for the unit profile, it is included in the card.

The default implementation creates the cards in the size of one-third of an A4 page, so 9 cards fit on one page for
printing.

The cards for the army code:

``hE0DanNhASCBLAIBAQAIAICQAQMAAICQAQQAAICTAQUAAIcZAQEAAIb%2FAQEAAIcZAQYAAICeAQIAAICeAQEAAgEABwCHHQEBAACHHAEDAACD6gEBAACApAECAACHIQECAACA3wEBAACAmAECAA%3D%3D``

look like this:

![img.png](img.png)

or as pdf: [JSA-inch.pdf](JSA-inch.pdf)

## Usage

Currently, you need to execute the application in an IDE, or you can download the JAR file, install Java 21, and execute
it with the following terminal command:

``java -jar Infinity-Data-VERSION.jar``

Open http://localhost:7070/ and input your army code. It is possible to select if the cards should be in inch or cm. It
is also possible the filter the cards to distinct units.

## Development

Contributions of any kind are welcome, especially if someone has experience in HTML layout and wants to help make the
cards look nicer.

Based on [Infinity-Army-Tools](https://github.com/cwoac/Infinity-Army-Tools)