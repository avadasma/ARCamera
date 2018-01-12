precision lowp float;
precision lowp int;
uniform sampler2D vTexture;
uniform int iternum;
uniform float aaCoef; //parameter
uniform float mixCoef; //Mixing coefficient
varying highp vec2 textureCoordinate;
varying highp vec2 blurCoord1s[14];
const float distanceNormalizationFactor = 4.0;    //Normalized distance factor constant
const mat3 saturateMatrix = mat3(1.1102,-0.0598,-0.061,-0.0774,1.0826,-0.1186,-0.0228,-0.0228,1.1772);

void main( ) {

    vec3 centralColor;
    float central;
    float gaussianWeightTotal;
    float sum;
    float sampleColor;
    float distanceFromCentralColor;
    float gaussianWeight;

    //Peel the skin through the green channel
    //Get the current color of the green channel
    central = texture2D( vTexture, textureCoordinate ).g;
    //Gaussian weight
    gaussianWeightTotal = 0.2;
    //Green channel color count
    sum = central * 0.2;

    // Calculate the Gaussian weights at each sampling point, including confinement and similarity
    for (int i = 0; i < 6; i++) {
        //Green channel for sample points
        sampleColor = texture2D( vTexture, blurCoord1s[i] ).g;
        //The color difference between the sample point and the calculated point
        distanceFromCentralColor = min( abs( central - sampleColor ) * distanceNormalizationFactor, 1.0 );
        //Gaussian weight
        gaussianWeight = 0.05 * (1.0 - distanceFromCentralColor);
        //Gaussian weight sum
        gaussianWeightTotal += gaussianWeight;
        //Green channel color count cumulative
        sum += sampleColor * gaussianWeight;
    }
    for (int i = 6; i < 14; i++) {
        //Green channel for sample points
        sampleColor = texture2D( vTexture, blurCoord1s[i] ).g;
        //The color difference between the sample point and the calculated point
        distanceFromCentralColor = min( abs( central - sampleColor ) * distanceNormalizationFactor, 1.0 );
        //Gaussian weight
        gaussianWeight = 0.1 * (1.0 - distanceFromCentralColor);
        //Gaussian weight sum
        gaussianWeightTotal += gaussianWeight;
        //Green channel color count cumulative
        sum += sampleColor * gaussianWeight;
    }

    //Sampling green channel color mean
    sum = sum / gaussianWeightTotal;

    //Get the color of the current point
    centralColor = texture2D( vTexture, textureCoordinate ).rgb;
    //sample value
    sampleColor = centralColor.g - sum + 0.5;
    //Iteration calculation
    for (int i = 0; i < iternum; ++i) {
        if (sampleColor <= 0.5) {
            sampleColor = sampleColor * sampleColor * 2.0;
        }else {
            sampleColor = 1.0 - ((1.0 - sampleColor)*(1.0 - sampleColor) * 2.0);
        }
    }

    float aa = 1.0 + pow( centralColor.g, 0.3 )*aaCoef;
    vec3 smoothColor = centralColor*aa - vec3( sampleColor )*(aa - 1.0);
    smoothColor = clamp( smoothColor, vec3( 0.0 ), vec3( 1.0 ) );
    smoothColor = mix( centralColor, smoothColor, pow( centralColor.g, 0.33 ) );
    smoothColor = mix( centralColor, smoothColor, pow( centralColor.g, mixCoef ) );
    gl_FragColor = vec4( pow( smoothColor, vec3( 0.96 ) ), 1.0 );
    vec3 satcolor = gl_FragColor.rgb * saturateMatrix;
    gl_FragColor.rgb = mix( gl_FragColor.rgb, satcolor, 0.23 );

}